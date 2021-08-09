package com.feng.hotel.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 订单房间 
 * </p>
 *
 * @author feng
 * @since 2021-08-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_order_room")
public class OrderRoom extends Model<OrderRoom> {

    private static final long serialVersionUID=1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 订单id
     */
    private Integer orderId;

    /**
     * 房间id
     */
    private String roomId;

    /**
     * 上间房子的订单房间id 仅做转房使用
     */
    private String pid;

    /**
     * 入住时间
     */
    @TableField("beginTime")
    private Date beginTime;

    /**
     * 离开时间
     */
    @TableField("engTime")
    private Date engTime;

    /**
     * 创建人
     */
    private Integer creator;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改人
     */
    private Integer modifier;

    /**
     * 修改时间
     */
    private Date modifyTime;

    /**
     * 是否删除 0删除
     */
    private Integer valid;


    public static final String ID = "id";

    public static final String ORDER_ID = "order_id";

    public static final String ROOM_ID = "room_id";

    public static final String PID = "pid";

    public static final String BEGINTIME = "beginTime";

    public static final String ENGTIME = "engTime";

    public static final String CREATOR = "creator";

    public static final String CREATE_TIME = "create_time";

    public static final String MODIFIER = "modifier";

    public static final String MODIFY_TIME = "modify_time";

    public static final String VALID = "valid";

    @Override
    protected Serializable pkVal() {
        return this.id;
    }

}