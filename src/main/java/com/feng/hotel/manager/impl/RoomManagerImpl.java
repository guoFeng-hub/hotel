package com.feng.hotel.manager.impl;

import com.feng.hotel.base.Constants;
import com.feng.hotel.base.exception.BizException;
import com.feng.hotel.base.exception.EnumReturnStatus;
import com.feng.hotel.common.HotelConstants;
import com.feng.hotel.common.enums.HotelEnum;
import com.feng.hotel.common.enums.RoomStatusEnum;
import com.feng.hotel.domain.Customer;
import com.feng.hotel.domain.OrderRoom;
import com.feng.hotel.domain.OrderRoomCustomer;
import com.feng.hotel.domain.PayRecord;
import com.feng.hotel.domain.Room;
import com.feng.hotel.manager.IRoomManager;
import com.feng.hotel.request.RoomRequest;
import com.feng.hotel.request.RoomSwapRequest;
import com.feng.hotel.response.CustomerResponse;
import com.feng.hotel.response.OrderRoomTreeResponse;
import com.feng.hotel.response.RoomResponse;
import com.feng.hotel.service.ICustomerService;
import com.feng.hotel.service.IOrderRoomCustomerService;
import com.feng.hotel.service.IOrderRoomService;
import com.feng.hotel.service.IOrderService;
import com.feng.hotel.service.IPayRecordService;
import com.feng.hotel.service.IRoomService;
import com.feng.hotel.utils.Assert;
import com.feng.hotel.utils.LambdaUtils;
import com.feng.hotel.utils.date.DateUtils;
import com.feng.hotel.utils.json.JsonUtils;
import com.feng.hotel.utils.tree.TreeUtils;
import com.feng.hotel.utils.tree.handler.TreeBuilderHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Administrator
 * @since 2021/8/3
 */
@Service
public class RoomManagerImpl implements IRoomManager {

    private final IRoomService roomService;

    private final IOrderService orderService;

    private final IOrderRoomCustomerService orderRoomCustomerService;

    private final ICustomerService customerService;

    private final IOrderRoomService orderRoomService;

    private final IPayRecordService payRecordService;

    public RoomManagerImpl(IRoomService roomService,
                           IOrderService orderService,
                           IOrderRoomCustomerService orderRoomCustomerService,
                           ICustomerService customerService,
                           IOrderRoomService orderRoomService, IPayRecordService payRecordService) {
        this.roomService = roomService;
        this.orderService = orderService;
        this.orderRoomCustomerService = orderRoomCustomerService;
        this.customerService = customerService;
        this.orderRoomService = orderRoomService;
        this.payRecordService = payRecordService;
    }

    @Override
    public void save(RoomRequest roomRequest, Long userNo) {
        Date date = new Date();
        Room room = JsonUtils.convert(roomRequest, Room.class);
        room.setStatus(RoomStatusEnum.NORMAL.name());
        room.setCreator(userNo);
        room.setModifier(userNo);
        room.setCreateTime(date);
        room.setModifyTime(date);
        room.setValid(Constants.Valid.NORMAL);
        roomService.save(room);
    }


    @Override
    public List<RoomResponse> list(String status) {
        //????????????
        List<Room> list = roomService.list(status);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }


        List<RoomResponse> roomResponses = JsonUtils.convertList(list, RoomResponse.class);

        //??????????????????
        Set<Long> orderIds = list.stream().map(Room::getOrderId).collect(Collectors.toSet());

        //???????????????????????????
        List<OrderRoom> orderRooms = this.orderRoomService.queryByOrderIds(orderIds).stream().filter(e -> !Objects.equals(e.getStatus(), HotelConstants.OrderStatus.CLOSE)).collect(Collectors.toList());
        Map<Long, OrderRoom> map = LambdaUtils.map(orderRooms, OrderRoom::getId);

        //??????????????????
        List<PayRecord> payRecords = payRecordService.queryByOrderIds(orderIds);
        HashMap<Long, Integer> orderPrice = countOrderPrice(orderRooms, payRecords);

        //??????????????????????????????
        Collection<OrderRoomCustomer> orderRoomCustomers = this.orderRoomCustomerService.queryByOrderRoomId(map.keySet());

        //??????????????????
        List<Customer> customers = customerService.queryByIds(
            orderRoomCustomers.stream().map(OrderRoomCustomer::getCustomerId).collect(Collectors.toSet()));
        Map<Long, Customer> customerMap = LambdaUtils.map(customers, Customer::getId);

        // key ??????????????????id value ????????????
        Map<Long, List<Customer>> longListMap = LambdaUtils.mapList(orderRoomCustomers, e -> {
            OrderRoom orderRoom = map.get(e.getOrderRoomId());
            return orderRoom.getRoomId();
        }, e -> customerMap.get(e.getCustomerId()));

        return roomResponses.stream().peek(e -> {
            e.setCustomers(JsonUtils.convertList(longListMap.get(e.getId()), CustomerResponse.class));
            e.setBalance(orderPrice.get(e.getOrderId()));
        }).collect(Collectors.toList());
    }

    /**
     * ??????????????????
     *
     * @param orderRooms     ????????????id
     * @param orderPayRecord ?????????????????????
     * @return key:??????id value:????????????
     */
    private HashMap<Long, Integer> countOrderPrice(List<OrderRoom> orderRooms, List<PayRecord> orderPayRecord) {
        Date date = new Date();
        //????????????????????????
        Map<Long, List<OrderRoom>> orderRoomMap = LambdaUtils.mapList(orderRooms, OrderRoom::getOrderId, e -> e);

        Map<Long, List<PayRecord>> orderPay = orderPayRecord.stream().collect(Collectors.groupingBy(PayRecord::getOrderId));

        HashMap<Long, Integer> orderPrice = new HashMap<>();
        //???????????????????????????????????????
        //?????????????????????08:00-?????????12?????????????????? ??????????????????????????????
        orderRoomMap.keySet().forEach(e -> {
            List<OrderRoomTreeResponse> orderRoomTreeResponses = new TreeBuilderHandler<>(JsonUtils.convertList(orderRoomMap.get(e), OrderRoomTreeResponse.class)).buildTree();
            int sum = TreeUtils.resolveAll(orderRoomTreeResponses).stream().mapToInt(i -> {
                int diffDays = 0;
               //????????????6??? ?????????
                if (i.getPid() == 0 && DateUtils.getHour(i.getBeginTime()) < 6) {
                    diffDays++;
                }

                //????????????
                Date endDate=Objects.isNull(i.getEngTime()) ? date : i.getEngTime();

                //????????????12??? ?????????
                diffDays += DateUtils.getDiffDays(i.getBeginTime(), endDate);
                if (DateUtils.getHour(endDate) > 12) {
                    diffDays++;
                }
                return i.getPrice() * diffDays;
            }).sum();

            int balance = orderPay.get(e).stream().mapToInt(PayRecord::getPrice).sum();

            orderPrice.put(e, balance - sum);
        });
        return orderPrice;
    }


    @Override
    public void updateStatus(Long id, String status, Long userId) {
        this.roomService.updateStatus(Collections.singleton(id), RoomStatusEnum.valueOf(status), userId);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void quit(Long roomId, Long userNo) {
        Room room = this.roomService.getById(roomId);
        Assert.assertNotNull(room, HotelEnum.ROOM_NOT_EXIST_ERROR);
        Assert.assertNotNull(room.getOrderId(), HotelEnum.ROOM_STATUS_ERROR);


        //????????????????????????????????????????????? ???????????? ??????????????????
        List<OrderRoom> orderRooms = orderRoomService.queryByOrderIds(Collections.singleton(room.getOrderId()));
        List<OrderRoom> orderRoomList = orderRooms.stream().filter(e -> Objects.equals(e.getStatus(), RoomStatusEnum.USING.name())).collect(Collectors.toList());
        if (orderRoomList.size() == 1) {
            orderService.updateStatus(room.getOrderId(), HotelConstants.OrderStatus.OUT, userNo);
        }

        //??????????????????
        roomService.updateStatus(Collections.singleton(roomId), RoomStatusEnum.READY_CLEAN, userNo);
        //??????????????????????????????
        orderRoomService.updateStatus(room.getOrderId(), roomId, HotelConstants.OrderStatus.OUT, userNo);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void swap(RoomSwapRequest roomSwapRequest, Long userNo) {
        Date date = new Date();

        //???????????????
        OrderRoom orderRoom = this.orderRoomService.getById(roomSwapRequest.getOrderRootId());
        if (Objects.isNull(orderRoom)) {
            throw new BizException(EnumReturnStatus.ROOM_NOT_EXIST);
        }

        //???????????????
        Room room = this.roomService.getById(roomSwapRequest.getNewRootId());
        //????????????????????????????????????
        if (!Objects.equals(roomSwapRequest.getNewRootId(), orderRoom.getRoomId()) && !Objects.equals(room.getStatus(), RoomStatusEnum.NORMAL.name())) {
            throw new BizException(EnumReturnStatus.ROOM_STATUS_ERROR);
        }


        //?????????????????????????????????
        this.roomService.swap(Collections.singleton(orderRoom.getRoomId()), userNo);

        //???????????????????????????
        this.orderRoomService.updateById(
            orderRoom.setStatus(HotelConstants.OrderStatus.OUT)
                .setModifier(userNo)
                .setModifyTime(date)
                .setEngTime(date)
        );

        //???????????????
        OrderRoom save = this.orderRoomService.save(orderRoom.getOrderId(), orderRoom.getId(), roomSwapRequest.getType(), roomSwapRequest.getPrice(), roomSwapRequest.getNewRootId(), userNo);

        //??????????????????????????????
        this.roomService.using(Collections.singleton(roomSwapRequest.getNewRootId()), save.getOrderId(), userNo);

        //??????????????????????????????
        //1??????????????????
        List<OrderRoomCustomer> orderRoomCustomers = this.orderRoomCustomerService.queryByOrderRoomId(Collections.singleton(orderRoom.getId()));
        this.orderRoomCustomerService.updateBatchById(
            orderRoomCustomers
                .stream()
                .peek(e ->
                    e.setStatus(HotelConstants.OrderStatus.OUT)
                        .setModifyTime(new Date())
                        .setModifier(userNo)
                ).collect(Collectors.toList())
        );

        //2?????????????????????
        this.orderRoomCustomerService.saveBatch(save.getId(), orderRoomCustomers.stream().map(OrderRoomCustomer::getCustomerId).collect(Collectors.toList()), userNo);

    }


}
