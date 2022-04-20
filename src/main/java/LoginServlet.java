import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet(urlPatterns = "/login")
public class LoginServlet extends HttpServlet {


    protected void doPost(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        Util.allowCrossOrigin(rsp);
        JSONObject jsonObj=new JSONObject();
        jsonObj.put("error",1);
        /**
         * 0，登录成功
         * 1，用户不存在或者密码错误
         * 2，登录状态过期
         * 11，服务器操作数据库错误
         */
        JSONObject reqPayload= JSON.parseObject( Util.parsePostPayload(req));
        String phone=reqPayload.getString("phone");
        String password=reqPayload.getString("password");
        int operType=reqPayload.getInteger("operType");
        /**
         * operType，操作类型
         * 1，用户在登录页面点击登录
         * 2，路由跳转前发起请求
         */
        switch (operType){
            case 1:
                MysqlQuery q=new MysqlQuery();
                q.selectFromDb("select * from user where phone=\"" + phone + "\" and password=\"" + password+"\"");
                try {
                    if(q.rs.next()) {
                        jsonObj.put("error", 0);
                        String uid=q.rs.getString("uid");
                        Cookie cookie=Util.setCookie("uid",uid,"/",48*3600);
                        rsp.addCookie(cookie);
                    }
                } catch (Exception e) {
                    System.out.println(e);
                    jsonObj.put("error",11);
                }
                q.closeDbConnection();
                break;
            case 2:
                Cookie[] cookies=req.getCookies();
                if(cookies!=null){
                    jsonObj.put("error",0);
                    jsonObj.put("uid",cookies[0].getValue());
                }else jsonObj.put("error",2);
                break;

        }

        rsp.setContentType("application/json");
        PrintWriter pw=rsp.getWriter();
        pw.write(jsonObj.toJSONString());
        pw.flush();
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        PrintWriter pw=rsp.getWriter();
        pw.write("{code:0}");
        pw.flush();
    }

    protected void doOptions(HttpServletRequest req,HttpServletResponse rsp){
        Util.allowCrossOrigin(rsp);
    }
}
