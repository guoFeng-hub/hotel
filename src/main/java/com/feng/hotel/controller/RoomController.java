package com.feng.hotel.controller;

import com.feng.hotel.base.entity.response.Result;
import com.feng.hotel.common.BaseController;
import com.feng.hotel.manager.IRoomManager;
import com.feng.hotel.request.RoomRequest;
import com.feng.hotel.request.RoomSwapRequest;
import com.feng.hotel.response.RoomResponse;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Administrator
 * @since 2021/8/3
 */
@RestController
@RequestMapping(value = "room")
public class RoomController extends BaseController {

    private final IRoomManager roomManager;

    public RoomController(IRoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @PostMapping(value = "save")
    @ApiOperation(value = "添加房间")
    public Result<?> save(@RequestBody @Validated RoomRequest roomRequest) {
        roomManager.save(roomRequest, super.getUserNo());
        return Result.success();
    }

    @GetMapping(value = "list")
    @ApiOperation(value = "房间列表")
    public List<RoomResponse> list(@RequestParam(value = "status", required = false) String status) {
        return roomManager.list(status);
    }


    @GetMapping(value = "update/status/{id}/{status}")
    @ApiOperation(value = "修改房间状态 ")
    public void post(@PathVariable(value = "id") @NotNull(message = "id不能为空") Long id,
                     @PathVariable(value = "status") @NotNull(message = "status不能为空") String status) {
        roomManager.updateStatus(id, status, super.getUserNo());
    }

    @GetMapping(value = "quit/{id}")
    @ApiModelProperty(value = "退房")
    public void quit(@PathVariable(value = "id") Long id) {
        roomManager.quit(id, super.getUserNo());
    }

    @GetMapping(value = "swap")
    @ApiModelProperty(value = "退房/改价")
    public void swap(@RequestBody RoomSwapRequest roomSwapRequest) {
        roomManager.swap(roomSwapRequest,super.getUserNo());
    }
}
