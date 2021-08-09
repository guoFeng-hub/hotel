package com.feng.hotel.service.impl;

import com.feng.hotel.base.Constants.Valid;
import com.feng.hotel.domain.OrderRoom;
import com.feng.hotel.mapper.OrderRoomMapper;
import com.feng.hotel.request.RoomUserRequest;
import com.feng.hotel.service.IOrderRoomService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.feng.hotel.utils.IdWorkerUtils;
import java.util.Date;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单房间  服务实现类
 * </p>
 *
 * @author feng
 * @since 2021-08-07
 */
@Service
public class OrderRoomServiceImpl extends ServiceImpl<OrderRoomMapper, OrderRoom> implements
    IOrderRoomService {

  @Override
  public void save(Long orderId, RoomUserRequest request, Long userNo) {
    Date date = new Date();
    this.save(new OrderRoom()
        .setId(IdWorkerUtils.generateLongId())
        .setOrderId(orderId)
        .setRoomId(request.getRoomId())
        .setBeginTime(date)
        .setCreateTime(date)
        .setModifyTime(date)
        .setCreator(userNo)
        .setModifier(userNo)
        .setValid(Valid.NORMAL)
    );

  }
}
