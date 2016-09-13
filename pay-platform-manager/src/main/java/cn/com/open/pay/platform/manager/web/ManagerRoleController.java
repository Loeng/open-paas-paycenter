package cn.com.open.pay.platform.manager.web;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import cn.com.open.pay.platform.manager.privilege.model.PrivilegeResource;
import cn.com.open.pay.platform.manager.privilege.model.PrivilegeRole;
import cn.com.open.pay.platform.manager.privilege.model.PrivilegeRoleDetails;
import cn.com.open.pay.platform.manager.privilege.model.TreeNode;
import cn.com.open.pay.platform.manager.privilege.service.PrivilegeModuleService;
import cn.com.open.pay.platform.manager.privilege.service.PrivilegeResourceService;
import cn.com.open.pay.platform.manager.privilege.service.PrivilegeRoleDetailsService;
import cn.com.open.pay.platform.manager.privilege.service.PrivilegeRoleService;
import cn.com.open.pay.platform.manager.tools.BaseControllerUtil;
import cn.com.open.pay.platform.manager.tools.WebUtils;
@Controller
@RequestMapping("/managerRole/")
public class ManagerRoleController  extends BaseControllerUtil {
	private static final Logger log = LoggerFactory.getLogger(UserLoginController.class);
	@Autowired
	private PrivilegeRoleService roleService;
	@Autowired
	private PrivilegeRoleDetailsService privilegeRoleDetailsService;
	@Autowired
	private PrivilegeModuleService privilegeModuleService;
	
	@Autowired
	private PrivilegeResourceService privilegeResourceService;
	/**
	 * 跳转到查询角色的页面
	 * @return 返回的是 jsp文件名路径及文件名
	 */
	@RequestMapping(value="roleMessage")
	public String showSearch(){
		log.info("-------------------------showSearch         start------------------------------------");
		return "privilege/role/roleMessage";
	}
	
	/**
	 * 根据条件实现查询角色操作
	 * @param request
	 * @param response
	 * @param model
	 * @return  查询完毕后返回查询结果
	 */
	@RequestMapping("QueryRoleMessage")
	public String QueryRoleMessage(HttpServletRequest request,HttpServletResponse response,Model model){
		log.info("-------------------------searchUser         start------------------------------------");
		//当前第几页
		String page=request.getParameter("page");
		//每页显示的记录数
	    String rows=request.getParameter("rows"); 
		//当前页  
		int currentPage = Integer.parseInt((page == null || page == "0") ? "1":page);  
		//每页显示条数  
		int pageSize = Integer.parseInt((rows == null || rows == "0") ? "10":rows);  
		//每页的开始记录  第一页为1  第二页为number +1   
	    int startRow = (currentPage-1)*pageSize;
		String roleName = request.getParameter("name");
		PrivilegeRole privilegeRole=new PrivilegeRole();
		privilegeRole.setName(roleName);
		privilegeRole.setPageSize(pageSize);
		privilegeRole.setStartRow(startRow);
		List<PrivilegeRole> privilegeRoleList=roleService.findByRoleName(privilegeRole);
		int countNum= roleService.findQueryCount(privilegeRole);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"); 
		for(int i=0;i<privilegeRoleList.size();i++){
			PrivilegeRole privilegeRole1 = privilegeRoleList.get(i);
			int status = privilegeRole1.getStatus();
			if(status==0){
				privilegeRoleList.get(i).setStatusName("禁用");
			}else{
				privilegeRoleList.get(i).setStatusName("启用");
			}
			Date createTime = privilegeRole1.getCreateTime();
			privilegeRole1.setCreate_Time(df.format(createTime));//交易时间
		}
		JSONArray jsonArr = JSONArray.fromObject(privilegeRoleList);
		 JSONObject jsonObjArr = new JSONObject();  
		 jsonObjArr.put("total", countNum);
		 jsonObjArr.put("rows", jsonArr);
		 System.out.println(jsonArr);
	     WebUtils.writeJson(response,jsonObjArr);
	     return "privilege/role/roleMessage";
	}
	
	
	 /**
     * 添加角色
     * @param request
     * @param model
     * @param bool
     * @return
     * @throws UnsupportedEncodingException 
     */
    @RequestMapping(value = "addRole")
	public void addRole(HttpServletRequest request,HttpServletResponse response) throws UnsupportedEncodingException {
    	String  id= request.getParameter("id");
    	String  status= request.getParameter("status");
    	String  temp= request.getParameter("temp");
    	if(temp!=""){
    		System.out.println("2");
    	}
    	
    	Map<String,Object> map = new HashMap<String, Object>();
    	String name=new String(request.getParameter("name").getBytes("iso-8859-1"),"utf-8");
    	if(id==""){
			List <PrivilegeRole> list= roleService.findByName(name);
			if(list!=null&& list.size()>0){
				map.put("returnMsg", "0");
			}else{
				PrivilegeRole privilegeRole=new PrivilegeRole();
				privilegeRole.setName(name);
				privilegeRole.setCreateTime(new Date());
				if(nullEmptyBlankJudge(status)){
					status="0";
				}
				privilegeRole.setStatus(Integer.parseInt(status));
				roleService.savePrivilegeRole(privilegeRole);
				int roleId = privilegeRole.getId();
				PrivilegeRoleDetails privilegeRoleDetails=new PrivilegeRoleDetails();
				if(temp!=""){
					String[] arr = temp.split(",");
					for(int i=0;i<arr.length;i++){
						String moduleId = arr[i];
						privilegeRoleDetails.setRoleId(roleId);
						privilegeRoleDetails.setModuleId(Integer.parseInt(moduleId));
						privilegeRoleDetailsService.savePrivilegeRole(privilegeRoleDetails);
					}
				}
				map.put("returnMsg", "1");
			}
    	}else{
    		PrivilegeRole privilegeRole=new PrivilegeRole();
    		privilegeRole.setId(Integer.parseInt(id));
    		privilegeRole.setName(name);
    		privilegeRole.setStatus(Integer.parseInt(status));
    		roleService.updatePrivilegeRole(privilegeRole);
    		map.put("returnMsg", "2");
    	}
    	WebUtils.writeErrorJson(response, map);
    }
    
    /**
     * 删除角色
     * @param request
     * @param model
     * @param bool
     * @return
     */
    @RequestMapping(value = "deleteRole")
	public void deleteRole(HttpServletRequest request,HttpServletResponse response,String id) {
    	String id1 = request.getParameter("id");
    	Map<String,Object> map = new HashMap<String, Object>();
    	try {
    		if(nullEmptyBlankJudge(id)){
       		 map.put("returnMsg", "0");//不存在	
       		}else{
       			roleService.deletePrivilegeRole(Integer.parseInt(id));
           		map.put("returnMsg", "1");//修改成功	
       		}
			} catch (Exception e) {
				 map.put("returnMsg", "0");//不存在	
			}
    	
    		
    	WebUtils.writeErrorJson(response, map);
    }
	
    
    @RequestMapping(value = "tree")
	public void getModelTree(HttpServletRequest request,HttpServletResponse response) {
		List<TreeNode> nodes = privilegeModuleService.getDepartmentTree();//见方法02
		 //buildTree(nodes)见方法01，return返回值自动转为json,不懂的话看下面紫色部分，懂的略过
		List<PrivilegeResource> privilegeResourceList = privilegeResourceService.findAllResource();
		JSONArray jsonArr = JSONArray.fromObject(buildTree(nodes,privilegeResourceList));
    	WebUtils.writeJson(response,jsonArr);
	}
    
    /**
	* 构建树
	* @param treeNodes
	* @return
	*/
	protected List<TreeNode> buildTree(List<TreeNode> treeNodes,List<PrivilegeResource> resourceList ) {
		List<TreeNode> results = new ArrayList<TreeNode>();

		Map<String, TreeNode> aidMap = new LinkedHashMap<String, TreeNode>();
		for (TreeNode node : treeNodes) {
			aidMap.put(node.getId(), node);
		}
		treeNodes = null;

		Set<Entry<String, TreeNode>> entrySet = aidMap.entrySet();
		for (Entry<String, TreeNode> entry : entrySet) {
			String pid = entry.getValue().getPid();
			TreeNode node = aidMap.get(pid);
			if (node == null) {
				results.add(entry.getValue());
			} else {
				List<TreeNode> children = node.getChildren();
				if (children == null) {
					children = new ArrayList<TreeNode>();
					node.setChildren(children);
					node.setState("closed");
				}
				else{
					//添加三级
					String resource = entry.getValue().getResource();
					List<TreeNode> treeNodeList = new ArrayList<TreeNode>();
					if(resource!=null){
						String[] resourceStrArray = resource.split(",");
						for(int i=0;i<resourceStrArray.length;i++){
							String vl = resourceStrArray[i];
							for(int j=0;j<resourceList.size();j++){
								int id = resourceList.get(j).getId();
								String nameValue = resourceList.get(j).getName();
								TreeNode treeNode=new TreeNode();
								if(Integer.parseInt(vl)==id){
									treeNode.setId(vl);
									treeNode.setText(nameValue);
//									treeNode.setState("closed");
									treeNodeList.add(treeNode);
								}
								
							}
						}
					}
					
					entry.getValue().setChildren(treeNodeList);
				}
				children.add(entry.getValue());
			}
		}
		aidMap = null;

		return results;
	}
	
}
