package org.gooru.insights.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gooru.insights.constants.APIConstants.*;
import org.gooru.insights.constants.APIConstants;
import org.gooru.insights.constants.InsightsOperationConstants;
import org.gooru.insights.constants.ResponseParamDTO;
import org.gooru.insights.security.AuthorizeOperations;
import org.gooru.insights.services.BaseConnectionServiceImpl;
import org.gooru.insights.services.ItemService;
import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value ="/query")
public class ItemController extends BaseController{
	
	@Autowired
	private ItemService itemService;
	
	/**
	 * This will check the tomcat service availability
	 * @param request
	 * @param response
	 * @return Model view object 
	 */
	@RequestMapping(value = "/server/status", method = RequestMethod.GET)
	public ModelAndView checkAPiStatus(HttpServletRequest request, HttpServletResponse response) {
		return getModel(getItemService().serverStatus());
	}

	/**
	 * This performs Elastic search query operation
	 * @param request HttpServlet Request 
	 * @param data This will hold the client request data
	 * @param sessionToken This is an Gooru sessionToken for validation
	 * @param response HttpServlet Response
	 * @return Model view object
	 * @throws Exception
	 */
	@RequestMapping(method = { RequestMethod.GET, RequestMethod.POST })
	@AuthorizeOperations(operations = InsightsOperationConstants.OPERATION_INSIHGHTS_REPORTS_VIEW)
	public ModelAndView generateQuery(HttpServletRequest request, @RequestParam(value = "data", required = true) String data,
			@RequestParam(value = "sessionToken", required = true) String sessionToken, HttpServletResponse response) throws Exception {

		return getModel(itemService.generateQuery(data, sessionToken, null));
	}
	
	@RequestMapping(value = "/{action}/report", method = RequestMethod.POST)
	@AuthorizeOperations(operations = InsightsOperationConstants.OPERATION_INSIHGHTS_REPORTS_VIEW)
	public ModelAndView manageReports(HttpServletRequest request, @PathVariable(value = "action") String action, @RequestParam(value = "reportName", required = true) String reportName,
			@RequestParam(value = "sessionToken", required = true) String sessionToken, @RequestBody String data, HttpServletResponse response) throws Exception {

		return getModel(itemService.manageReports(action, reportName, data));
	}
	
	/**
	 * 
	 * @param request
	 * @param reportType
	 * @param sessionToken
	 * @param response
	 * @return Model view object
	 * @throws Exception
	 */
	@RequestMapping(value = "/report/{reportType}", method = { RequestMethod.GET, RequestMethod.POST })
	@AuthorizeOperations(operations = InsightsOperationConstants.OPERATION_INSIHGHTS_REPORTS_VIEW)
	public ModelAndView getPartyReports(HttpServletRequest request, @PathVariable(value = "reportType") String reportType, @RequestParam(value = "sessionToken", required = true) String sessionToken,
			HttpServletResponse response) throws Exception {

		return getModel(itemService.getPartyReport(request, reportType, sessionToken));
	}
	
	/**
	 * This will clear the stored query in redis
	 * @param request is the client HTTPRequest
	 * @param queryId is the unique query id for each query
	 * @param response is the client HTTPResponse
	 * @return Model view object
	 */
	@RequestMapping(value="/clear/id",method =RequestMethod.GET)
	public ModelAndView clearRedisCache(HttpServletRequest request,@RequestParam(value="queryId",required = true) String queryId,HttpServletResponse response){
	
		return getModel(itemService.clearQuery(queryId));
	}
	
	/**
	 * This will provide the query result for the given query id
	 * @param request is the client HTTPRequest
	 * @param queryId is the unique query id for each query
	 * @param sessionToken is the Gooru user token
	 * @param response is the client HTTPResponse
	 * @return Model view object
	 */
	@RequestMapping(value="/{id}",method =RequestMethod.GET)
	@AuthorizeOperations(operations =  InsightsOperationConstants.OPERATION_INSIHGHTS_REPORTS_VIEW)
	public ModelAndView getRedisCache(HttpServletRequest request,@PathVariable("id") String queryId,@RequestParam(value="sessionToken",required = true) String sessionToken,HttpServletResponse response){
		
		return getModel(itemService.getQuery(queryId,sessionToken));
	}

	/**
	 * This will provide the list of query result for the given query id or the whole item
	 * @param request is the client HTTPRequest
	 * @param queryId is the unique query id for each query
	 * @param sessionToken is the Gooru user token
	 * @param response is the client HTTPResponse
	 * @return Model view object
	 */
	@RequestMapping(value="/list",method =RequestMethod.GET)
	@AuthorizeOperations(operations =  InsightsOperationConstants.OPERATION_INSIHGHTS_REPORTS_VIEW)
	public ModelAndView getRedisCacheList(HttpServletRequest request,@RequestParam(value="queryId",required = false) String queryId,@RequestParam(value="sessionToken",required = true) String sessionToken,HttpServletResponse response){
		 
		return getModel(getItemService().getCacheData(queryId,sessionToken));
	}
	
	/**
	 * 
	 * @param request
	 * @param data
	 * @param response
	 * @return Model view object
	 */
	@RequestMapping(value="/keys",method =RequestMethod.PUT)
	@AuthorizeOperations(operations =  InsightsOperationConstants.OPERATION_INSIHGHTS_REPORTS_VIEW)
	public ModelAndView putRedisData(HttpServletRequest request,@RequestBody String data ,HttpServletResponse response){
		
		return getModel(getItemService().insertKey(data));
	}
	
	/**
	 * This will clear the cached data
	 * @return Model view object
	 */
	@RequestMapping(value="/clear/data",method =RequestMethod.GET)
	@AuthorizeOperations(operations =  InsightsOperationConstants.OPERATION_INSIHGHTS_REPORTS_VIEW)
	public ModelAndView clearDataCache(){
		
		return getModel(getItemService().clearDataCache());
	}
	
	/**
	 * This will combine the two API call and project it as single
	 * @param request is the client HTTPRequest
	 * @param data is the API query to fetch data from ELS
	 * @param sessionToken is the Gooru user token
	 * @param response response is the client HTTPResponse
	 * @return Model view object
	 * @throws Exception
	 */
	@RequestMapping(value="/combine",method ={RequestMethod.GET,RequestMethod.POST})
	@AuthorizeOperations(operations =  InsightsOperationConstants.OPERATION_INSIHGHTS_REPORTS_VIEW)
	public ModelAndView getItems(HttpServletRequest request,@RequestParam(value="data",required = true) String data,@RequestParam(value="sessionToken",required = true) String sessionToken,HttpServletResponse response) throws Exception{
		
		return getModel(getItemService().processApi(data,sessionToken));
	}
	
	/**
	 * This will clear the data connection of cassandra and ELS
	 * @return Model view object
	 */
	@RequestMapping(value="/clear/connection",method =RequestMethod.GET)
	public ModelAndView clearConnectionCache(){
		return getModel(getItemService().clearConnectionCache());
	}

	public ItemService getItemService() {
		return itemService;
	}
}
