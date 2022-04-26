import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(urlPatterns = "/login")
public class LoginServlet extends HttpServlet {


    protected void doPost(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        Util.allowCrossOrigin(rsp);
        JSONObject jsonObj=new JSONObject();
        jsonObj.put("error",1);
        /**
         * 0，登录页点击登录成功
         * 1，用户不存在或者密码错误
         * 2，cookie过期
         * 3，uid过期，如果用户在同一浏览器页面A登录A账号，然后在页面B退出并登录B账号，再返回A页面，此时会发生A页面uid和cookie不一致
         * 4,cookie和uid匹配正常
         * 5，用户在cookie有效期内重新打开页面
         * 11，服务器操作数据库错误
         */
        JSONObject reqPayload= JSON.parseObject( Util.parsePostPayload(req));
        String phone=reqPayload.getString("phone");
        String password=reqPayload.getString("password");
        int operType=reqPayload.getInteger("operType");
        /**
         * operType，操作类型
         * 1，用户在登录页面点击登录
         * 2，检测cookie和uid是否一致的请求
         */
        switch (operType){
            case 1:
                MysqlQuery q=new MysqlQuery();
                q.selectFromDb("select * from user where phone=\"" + phone + "\" and password=\"" + password+"\"");
                try {
                    if(q.rs.next()) {
                        jsonObj.put("error", 0);
                        String uid=q.rs.getString("uid");
                        jsonObj.put("uid",uid);
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
                String uid=reqPayload.getString("uid");
                if(cookies==null) jsonObj.put("error",2);
                else if(uid.equals("")) {
                    jsonObj.put("error",5);
                    jsonObj.put("uid",cookies[0].getValue());
                }
                else if(!uid.equals(cookies[0].getValue())){
                    jsonObj.put("error",3);
                    jsonObj.put("uid",cookies[0].getValue());
                }else jsonObj.put("error",4);
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
