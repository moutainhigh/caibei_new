package com.borrow.supermarket.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.borrow.supermarket.dao.OrderDao;
import com.borrow.supermarket.model.LendModel;
import com.borrow.supermarket.model.OrderModel;
import com.borrow.supermarket.model.UserModel;
import com.borrow.supermarket.mybatis.PageParameter;
import com.borrow.supermarket.request.dto.GetNewOrderRequestDTO;
import com.borrow.supermarket.request.dto.GetsaveOrderRequestDTO;
import com.borrow.supermarket.response.result.GetUserOrdersResultDTO;
import com.borrow.supermarket.response.result.GetsaveOrderDTOResponse;
import com.borrow.supermarket.util.PageWebDTOResult;
import com.borrow.supermarket.util.ResponseEntity;
import com.borrow.supermarket.util.ServiceCode;

@Service
public class OrderService
{
	  private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

	  @Autowired
	  private OrderDao orderDaoI;

	  @Autowired
	  private UserService userServiceI;

	  @Autowired
	  private LendService lendServiceI;

	  public List<OrderModel> selectOrderList(OrderModel orderModel, PageParameter parameter) { try { Map map = new HashMap();
	      map.put("t", orderModel);
	      map.put("page", parameter);
	      return this.orderDaoI.getByPage(map);
	    } catch (Exception e)
	    {
	      e.printStackTrace();
	      logger.error("订单列表展示操作异常---原因是-----:" + e.getMessage());
	    }return null;
	  }

	  public OrderModel getOrderById(OrderModel orderModel)
	  {
	    try
	    {
	      return (OrderModel)this.orderDaoI.getInfo(orderModel);
	    } catch (Exception e) {
	      e.printStackTrace();
	      logger.error("查看借款订单详情操作异常---原因是-----:" + e.getMessage());
	    }return null;
	  }

	  public boolean updateCreditStatus(OrderModel orderModel)
	  {
	    try
	    {
	      return this.orderDaoI.updateOrderStatus(orderModel) > 0;
	    } catch (Exception e) {
	      e.printStackTrace();
	      logger.error("参与返现审核状态操作异常---原因是-----:" + e.getMessage());
	    }return false;
	  }

	  public List getUserOrders(String userIdentifier)
	  {
	    try
	    {
	      List<GetUserOrdersResultDTO> userOrderList = this.orderDaoI.getUserOrders(userIdentifier);//MJW
	      for (GetUserOrdersResultDTO userOrder : userOrderList) {
	        if (userOrder.getCashbackInfo().equals("1")) {
	          userOrder.setCashbackInfo("上传截图获取花费");
	        }

	        if (userOrder.getCashbackInfo().equals("2")) {
	          userOrder.setCashbackInfo("还有2天审核通过");
	        }

	        if (userOrder.getCashbackInfo().equals("3")) {
	          userOrder.setCashbackInfo("审核通过今日返现请留意");
	        }
	        if (userOrder.getCashbackInfo().equals("4")) {
	          userOrder.setCashbackInfo("返回中请注意查收");
	        }
	        if (userOrder.getCashbackInfo().equals("已经返回")) {
	          userOrder.setCashbackInfo("审核不通过请重新上传");
	        }
	      }
	      return userOrderList;
	    } catch (Exception e) {
	      logger.error("用户个人中心返现订单列表查询操作异常---原因是-----:" + e.getMessage());
	    }return null;
	  }

	  @Transactional
	  public ResponseEntity saveUserOrder(GetsaveOrderRequestDTO getsaveOrderRequestDTO, String userIdentifier)
	  {
	    ResponseEntity responseEntity = new ResponseEntity();
	    try {
	      UserModel userModel = this.userServiceI.searchUserByUserIdentifier(userIdentifier);
	      if (userModel == null) {
	        responseEntity.setMsg(ServiceCode.SING_IN_REPONSE_ONE);
	        return responseEntity;
	      }

	      LendModel lendModel = this.lendServiceI.getLendByIdentifier(new LendModel(getsaveOrderRequestDTO));
	      if (lendModel == null) {
	        responseEntity.setMsg(ServiceCode.SAVEORDER_ONE);
	        return responseEntity;
	      }
	      int returnResult = this.orderDaoI.save(new OrderModel(userModel, lendModel, getsaveOrderRequestDTO)).intValue();
	      if (returnResult <= 0) {
	        responseEntity.setMsg(ServiceCode.ERROR);
	        return responseEntity;
	      }

	      boolean flag = this.lendServiceI.updatetotalApply(lendModel.getId());
	      if (!flag) {
	        throw new RuntimeException();
	      }
	      
	      int dayTotalApply = this.lendServiceI.getDayTotalApply(lendModel.getId());
	      if (dayTotalApply < 0) {
	    	throw new RuntimeException();
	      }
	      
	      GetsaveOrderDTOResponse saveOrder = new GetsaveOrderDTOResponse();
	      saveOrder.setIdentifier(getsaveOrderRequestDTO.getIdentifier());
	      saveOrder.setLendUrl(lendModel.getLendUrl());
	      saveOrder.setDayTotalApply(dayTotalApply);
	      responseEntity.addProperty(saveOrder);
	      return responseEntity;
	    } catch (Exception e) {
	      responseEntity.setMsg(ServiceCode.EXCEPTION);
	    }return responseEntity;
	  }

	  public GetNewOrderRequestDTO newOrderInfo()
	  {
	    try {
	      GetNewOrderRequestDTO getOrderInfo1 = this.orderDaoI.getNewOrderInfo();
	      GetNewOrderRequestDTO getOrderInfo2 = this.orderDaoI.getTotalLendMoneyInfo();
	      GetNewOrderRequestDTO getOrderInfo3 = this.orderDaoI.getTotalLendPersonInfo();
	      getOrderInfo1.setServicePersonTime(getOrderInfo3.getServicePersonTime());
	      getOrderInfo1.setTotalLendMoney(getOrderInfo2.getTotalLendMoney());
	      return getOrderInfo1;
	    } catch (Exception e) {
	      logger.error("获取最新订单信息操作异常---原因是-----:" + e.getMessage());
	    }return null;
	  }
	  
	  // add by mjw 首页信息(使用ObjectMapper)太局限
/*	  @SuppressWarnings("unchecked")
	public PageWebDTOResult<HomeMessDisDTO<ProductDTO>> getHomeMessage1()
	  {
	    PageParameter pageweb = new PageParameter();
	    String homeMessage = this.orderDaoI.getHomeMessage();
	    ObjectMapper om = new ObjectMapper();
	    om.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);// 转义字符-异常情况
	    om.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);// 
	    List<HomeMessDisDTO<ProductDTO>> homeMessDisDTO = new ArrayList<HomeMessDisDTO<ProductDTO>>();
	    HomeMessDisDTO<ProductDTO>[] jsonArray = null;
	    PageWebDTOResult<HomeMessDisDTO<ProductDTO>> pageWebDTOResult = new PageWebDTOResult<HomeMessDisDTO<ProductDTO>>(pageweb.getPageSize(), pageweb.getPageNow(), pageweb.getRowCount(), pageweb.getPageCount(), homeMessDisDTO);
		try {
			jsonArray = om.readValue(homeMessage, HomeMessDisDTO[].class);
			for(HomeMessDisDTO<ProductDTO> homeMess: jsonArray){
				homeMessDisDTO.add(homeMess);
			}
			pageWebDTOResult.setListData(homeMessDisDTO);
			return pageWebDTOResult;
		} catch (IOException e) {
			pageWebDTOResult.setMsg(ServiceCode.EXCEPTION);
			e.printStackTrace();
		}
	    return pageWebDTOResult;
	  }*/
	  
	  // 获取首页数据(使用JSONObject)
	public PageWebDTOResult<Object> getContent(Integer type) {
		PageParameter pageweb = new PageParameter();
		List<Object> homeMessDisDTO = new ArrayList<Object>();
		PageWebDTOResult<Object> pageWebDTOResult = new PageWebDTOResult<Object>(pageweb.getPageSize(), pageweb.getPageNow(), pageweb.getRowCount(), pageweb.getPageCount(), homeMessDisDTO);
		try {
			String homeMessage = this.orderDaoI.getContent(type);
			if ((homeMessage != null) && (homeMessage.trim().charAt(0) == '[')) {
				JSONArray ja = JSONArray.fromObject(homeMessage);
				if (ja.size() > 0) {
					for (int i = 0; i < ja.size(); i++) {
						JSONObject job = ja.getJSONObject(i);
						homeMessDisDTO.add(job);
					}
				}
			} else {
				JSONObject jb = JSONObject.fromObject(homeMessage);
				homeMessDisDTO.add(jb);
			}
			pageWebDTOResult.setListData(homeMessDisDTO);
			return pageWebDTOResult;
		} catch (Exception e) {
			pageWebDTOResult.setMsg(ServiceCode.EXCEPTION);
			e.printStackTrace();
		}
		return pageWebDTOResult;
	}
}