package cn.smbms.controller;

import cn.smbms.pojo.Role;
import cn.smbms.pojo.User;
import cn.smbms.service.role.RoleService;
import cn.smbms.service.user.UserService;
import cn.smbms.service.user.UserServiceImpl;
import cn.smbms.tools.Constants;
import cn.smbms.tools.PageSupport;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.mysql.jdbc.StringUtils;
import com.sun.org.apache.xpath.internal.operations.Mod;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private RoleService roleService;

    //实现跳转到登陆页
    @RequestMapping(value = "/login.html")
    public String login() {
        System.out.println(1);
        return "login";
    }

    //实现登陆
    @RequestMapping(value = "dologin.html", method = RequestMethod.POST)
    //登陆成功--》放入Httpsession中    登陆失败--》HttpServletRequest
    public String dologin(@RequestParam String userCode,
                          @RequestParam String userPassword,
                          HttpSession session,
                          HttpServletRequest request) {
        //调用Service方法  进行用户匹配
        User user = userService.login(userCode, userPassword);
        if (user != null) {//登陆成功
            //把当前用户放入session中
            session.setAttribute(Constants.USER_SESSION, user);
            //页面跳转--跳转到首页(frame.jsp)
            System.out.println(2);
            return "redirect:/user/main.html";//相当于重定向：Response.sendRedirect("/frame.jsp")
        } else {
            //页面跳转（login.jsp） 带出提示信息
            request.setAttribute("error", "用户名或密码不正确");
            System.out.println(3);
            return "login";
        }
    }

    //对登陆进一步验证  防止跳过登陆   或防止session已过期
    @RequestMapping(value = "main.html")
    public String main(HttpSession session) {
        if (session.getAttribute(Constants.USER_SESSION) == null) {
            System.out.println(4);
            return "redirect:/user/login.html";
        }
        System.out.println(5);
        return "frame";
    }

    @RequestMapping(value = "/exlogin.html", method = RequestMethod.GET)
    public String exLogin(@RequestParam String userCode,
                          @RequestParam String userPassword) {
        //调用service方法，进行用户匹配
        System.out.println("进入异常");
        User user = userService.login(userCode, userPassword);
        if (null == user) {//登录失败
            System.out.println("异常");
            throw new RuntimeException("用户名或者密码不正确！");
        }
        return "redirect:/user/main.html";
    }

    @ExceptionHandler(value = {RuntimeException.class})
    public String handlerException(RuntimeException e, HttpServletRequest req) {
        req.setAttribute("e", e);
        return "error";
    }

    //注销
    @RequestMapping("/loginOut")
    public String loginOut(HttpSession session) {
        //清除session
        session.removeAttribute(Constants.USER_SESSION);
        return "login";
    }

    //查询用户列表
    @RequestMapping(value = "/userlist.html")
    public String getUserList(Model model,
                              @RequestParam(value = "queryname", required = false) String queryUserName,
                              @RequestParam(value = "queryUserRole", required = false) String queryUserRole,
                              @RequestParam(value = "pageIndex", required = false) String pageIndex) {
        System.out.println("获取用户列表");
        //查询用户列表
        int _queryUserRole = 0;
        List<User> userList = null;
        //设置页面容量
        int pageSize = Constants.pageSize;
        //当前页码
        int currentPageNo = 1;

        if (queryUserName == null) {
            queryUserName = "";
        }
        if (queryUserRole != null && !queryUserRole.equals("")) {
            _queryUserRole = Integer.parseInt(queryUserRole);
        }

        if (pageIndex != null) {
            try {
                currentPageNo = Integer.valueOf(pageIndex);
            } catch (NumberFormatException e) {
                return "redirect:/user/syserror.html";
                //response.sendRedirect("syserror.jsp");
            }
        }
        //总数量（表）
        int totalCount = userService.getUserCount(queryUserName, _queryUserRole);
        //总页数
        PageSupport pages = new PageSupport();
        pages.setCurrentPageNo(currentPageNo);
        pages.setPageSize(pageSize);
        pages.setTotalCount(totalCount);
        int totalPageCount = pages.getTotalPageCount();
        //控制首页和尾页
        if (currentPageNo < 1) {
            currentPageNo = 1;
        } else if (currentPageNo > totalPageCount) {
            currentPageNo = totalPageCount;
        }
        userList = userService.getUserList(queryUserName, _queryUserRole, currentPageNo, pageSize);
        model.addAttribute("userList", userList);
        //下拉列表框的内容
        List<Role> roleList = null;
        roleList = roleService.getRoleList();
        model.addAttribute("roleList", roleList);
        model.addAttribute("queryUserName", queryUserName);
        model.addAttribute("queryUserRole", queryUserRole);
        model.addAttribute("totalPageCount", totalPageCount);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("currentPageNo", currentPageNo);
        return "userlist";
    }

    @RequestMapping(value = "/syserror.html")
    public String sysError() {
        return "syserror";
    }

    //新增用户
    @RequestMapping(value = "/useradd.html", method = RequestMethod.GET)
    public String adduser(User user, Model model) {
        model.addAttribute("user", user);
        return "useradd";
    }
////保存用户信息 单文件上传
//@RequestMapping(value = "/addsave.html",method = RequestMethod.POST)
//    public String addUserSave(User user,
//                              HttpSession session,
//                              HttpServletRequest request,
//                              @RequestParam(value = "a_idPicPath",required = false)MultipartFile attach){
//      String idPicPath=null;
//        //判断文件是否为空
//    if (!attach.isEmpty()){
////定义上传目标路径
//        String path=request.getSession().getServletContext().getRealPath("statics"+ File.separator+"uploadfiles");
//        //获取原文件名
//        String oldFileName=attach.getOriginalFilename();
//        //获取原文件后缀
//        String prefix= FilenameUtils.getExtension(oldFileName);
//        //规定上传文件大小
//        int filesize=500000;
//        //对后缀进行判断
//        if (attach.getSize() > filesize) {
//            request.setAttribute("uploadFileError"," * 上传大小不得超过500K");
//            return "useradd";
//        }else if (prefix.equalsIgnoreCase("jpg")
//                ||prefix.equalsIgnoreCase("jpeg")
//                ||prefix.equalsIgnoreCase("png")
//                ||prefix.equalsIgnoreCase("pneg")){
//         //当前系统时间+随机数+"_Personal.jpg"
//            String fileName = System.currentTimeMillis() + (int) (Math.random() * 1000000) + "_Personal.jpg";
//            //                //path：上传路径  fileName：文件名
//               File targetFile = new File(path, fileName);
//            //判断是否存在
//                //若不存在则创建文件
//                if (!targetFile.exists()) {
//                    targetFile.mkdirs();
//                }
//                //保存
//                try {
//                    //接收用户上传的文件流
//                    attach.transferTo(targetFile);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    request.setAttribute("uploadFileError", "上传失败");
//                    return "useradd";
//                }
//                //更新数据库中的idPicPath:路径
//                idPicPath = path + File.separator + fileName;
//                // 如果格式不正确
//        }else {
//            request.setAttribute("uploadFileError"," * 上传图片格式不正确");
//            return "useradd";
//        }
//
//    }
//
//
//
//
//
//
//
//        //干什么用的？？？
//        user.setCreatedBy(((User)session.getAttribute(Constants.USER_SESSION)).getId());
//    user.setCreationDate(new Date());
//    user.setIdPicPath(idPicPath);
//    if (userService.add(user)){
//        return "redirect:/user/userlist.html";
//    }
//    return "useradd";
//}

    //保存用户信息 多文件上传
    @RequestMapping(value = "/addsave.html", method = RequestMethod.POST)
    public String addUserSave(User user,
                              HttpSession session,
                              HttpServletRequest request,
                              @RequestParam(value = "attachs", required = false) MultipartFile[] attachs) {
        String idPicPath = null;
        String workPicPath = null;
        String errorInfo = null;
        boolean flag = true;
        //定义上传目标路径
        String path = request.getSession().getServletContext().getRealPath("statics" + File.separator + "uploadfiles");
        for (int i = 0; i < attachs.length; i++) {
            MultipartFile attach = attachs[i];
            //判断文件是否为空
            if (!attach.isEmpty()) {
                if (i == 0) {
                    errorInfo = "uploadFileError";
                } else if (i == 1) {
                    errorInfo = "uploadWpError";
                }
                //获取原文件名
                String oldFileName = attach.getOriginalFilename();
                //获取原文件后缀
                String prefix = FilenameUtils.getExtension(oldFileName);
                //规定上传文件大小
                int filesize = 500000;
                //对后缀进行判断
                if (attach.getSize() > filesize) {
                    request.setAttribute(errorInfo, " * 上传大小不得超过500K");
                    flag = false;
                } else if (prefix.equalsIgnoreCase("jpg")
                        || prefix.equalsIgnoreCase("jpeg")
                        || prefix.equalsIgnoreCase("png")
                        || prefix.equalsIgnoreCase("pneg")) {
                    //当前系统时间+随机数+"_Personal.jpg"
                    String fileName = System.currentTimeMillis() + (int) (Math.random() * 1000000) + "_Personal.jpg";
                    //                //path：上传路径  fileName：文件名
                    File targetFile = new File(path, fileName);
                    //判断是否存在
                    //若不存在则创建文件
                    if (!targetFile.exists()) {
                        targetFile.mkdirs();
                    }
                    //保存
                    try {
                        //接收用户上传的文件流
                        attach.transferTo(targetFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                        request.setAttribute(errorInfo, "上传失败");
                        flag = false;
                    }
                    if (i == 0) {
                        //更新数据库中的idPicPath:路径
                        idPicPath = path + File.separator + fileName;
                    } else if (i == 1) {
                        workPicPath = path + File.separator + fileName;
                    }

                    // 如果格式不正确
                } else {
                    request.setAttribute(errorInfo, " * 上传图片格式不正确");
                    flag = false;
                }

            }
        }
        if (flag) {
            //干什么用的？？？
            user.setCreatedBy(((User) session.getAttribute(Constants.USER_SESSION)).getId());
            user.setCreationDate(new Date());
            user.setIdPicPath(idPicPath);
            user.setWorkPicPath(workPicPath);
            //添加操作
            if (userService.add(user)) {
                return "redirect:/user/userlist.html";
            }
        }
        return "useradd";
    }

    //根据用户ID获取用户信息
    @RequestMapping(value = "/usermodify.html", method = RequestMethod.GET)
    public String getUserById(@RequestParam String uid, Model model) {
        User user = userService.getUserById(uid);
        model.addAttribute(user);
        return "usermodify";
    }

    //保存修改的用户信息
    @RequestMapping(value = "usermodifysave.html", method = RequestMethod.POST)
    public String modifyUserSave(User user, HttpSession session) {
        //更新者
        user.setModifyBy(((User) session.getAttribute(Constants.USER_SESSION)).getId());
        //更新时间
        user.setModifyDate(new Date());
        if (userService.modify(user)) {
            return "redirect:/user/userlist.html";
        }
        return "usermodify";
    }

    //通过用户ID查询用户信息
    @RequestMapping(value = "/view/{id}", method = RequestMethod.GET)
    public String view(@PathVariable String id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute(user);
        return "userview";
    }

    //删除用户
    @ResponseBody
    @RequestMapping("deluser")
    private String delUser(HttpServletRequest request, String uid) {
        Integer delId = 0;
        try {
            delId = Integer.parseInt(uid);
        } catch (Exception e) {
            // TODO: handle exception
            delId = 0;
        }
        HashMap<String, String> resultMap = new HashMap<String, String>();
        if (delId <= 0) {
            resultMap.put("delResult", "notexist");
        } else {
            if (userService.deleteUserById(delId)) {
                resultMap.put("delResult", "true");
            } else {
                resultMap.put("delResult", "false");
            }
        }

        //把resultMap转换成json对象输出
        return JSONArray.toJSONString(resultMap);
    }
//修改密码
    @RequestMapping("pwdmodify")
    public String pwdModify(){
        return "pwdmodify";
    }
    @RequestMapping(value = "updatepwd",method = RequestMethod.POST)
public String updatePwd(HttpServletRequest request,String newpassword){

    Object o = request.getSession().getAttribute(Constants.USER_SESSION);
    boolean flag = false;
    if(o != null && !StringUtils.isNullOrEmpty(newpassword)){
        flag = userService.updatePwd(((User)o).getId(),newpassword);
        if(flag){
            request.setAttribute(Constants.SYS_MESSAGE, "修改密码成功,请退出并使用新密码重新登录！");
            request.getSession().removeAttribute(Constants.USER_SESSION);//session注销
        }else{
            request.setAttribute(Constants.SYS_MESSAGE, "修改密码失败！");
        }
    }else{
        request.setAttribute(Constants.SYS_MESSAGE, "修改密码失败！");
    }
    return "pwdmodify";
}
    @ResponseBody
    @RequestMapping(value = "oldPwdQuery",produces = "application/json;charset=UTF-8")
public String getPwdByUserId(HttpServletRequest request,String oldpassword){
    Object o = request.getSession().getAttribute(Constants.USER_SESSION);
    Map<String, String> resultMap = new HashMap<String, String>();

    if(null == o ){//session过期
        resultMap.put("result", "sessionerror");
    }else if(StringUtils.isNullOrEmpty(oldpassword)){//旧密码输入为空
        resultMap.put("result", "error");
    }else{
        String sessionPwd = ((User)o).getUserPassword();
        if(oldpassword.equals(sessionPwd)){
            resultMap.put("result", "true");
        }else{//旧密码输入不正确
            resultMap.put("result", "false");
        }
    }

    return JSONArray.toJSONString(resultMap);
}

    //JSON对象
    @RequestMapping(value = "/ucexist.html")
    @ResponseBody
    public Object userCodeIsExist(@RequestParam String userCode) {
        HashMap<String, String> resultMap = new HashMap<String, String>();
        //判断userCode是否为空
        if (StringUtils.isNullOrEmpty(userCode)) {
            //如果为空
            resultMap.put("userCode", "exist");

        } else {
            User user = userService.selectUserCodeExist(userCode);
            if (user != null) {
                resultMap.put("userCode", "exist");
            } else {
                resultMap.put("userCode", "noexist");
            }
        }
        return JSONArray.toJSONString(resultMap);
    }

//,produces = {"application/json;charset=UTF-8"}
    @RequestMapping(value = "/view",method = RequestMethod.GET)
    @ResponseBody
    //根据id获取用户对象
    public Object view(@RequestParam String id){
        String cjson="";
        if(id==null||"".equals(id)){
            return "nodata";
        }else{
            try {
                User user=userService.getUserById(id);
                cjson=JSON.toJSONString(user);
            }catch (Exception e){
                e.printStackTrace();
                return "failed";
            }
        }
        return cjson;
    }
//111111181111
}
