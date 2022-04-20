import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Date;
import java.util.Random;

@WebServlet(urlPatterns = "/register")
public class RegisterServlet extends HttpServlet {


    protected void doPost(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        Util.allowCrossOrigin(rsp);
        rsp.setContentType("application/json");
        JSONObject rspObj=new JSONObject();
        rspObj.put("error",0);
        /**
         * 0，注册成功
         * 1,用户名已存在
         * 2，手机号已经注册
         * 3，手机号不正确
         * 4,手机号未注册
         * 5，校验码错误
         * 6，发送验证码成功
         * 7，发送验证码失败
         * 8，用户名不存在
         * 9，校验码正确
         * 10，重置密码成功
         * 11，服务器操作数据库时错误
         */
        JSONObject reqPayload= JSON.parseObject(Util.parsePostPayload(req));
        String userName=reqPayload.getString("userName");
        String phone=reqPayload.getString("phone");  //前端已保证是11位数字，但不保证是否已经注册
        String password=reqPayload.getString("password");
        String verifyCode=reqPayload.getString("verifyCode");
        int operType=reqPayload.getInteger("operType");
        /**
         * operType=1，重置密码获取验证码请求，首先需要校验手机号是否已经注册
         * operType=2 注册时获取验证码请求，校验手机号未注册
         * operType=3，校验用户名是否已被使用
         * operType=4，注册请求，校验验证码是否正确、用户名是否存在、手机号是否已经注册
         * operType=5，重置密码请求，校验手机号要注册过，验证码要正确
         */
        int checkPhoneStatus;
        int checkVerifyCodeStatus;
        int checkUserNameStatus;
        switch (operType){
            case 1:
                checkPhoneStatus=checkPhoneExist(phone);
                if(checkPhoneStatus==2){
                    rspObj.put("error",sendVerifyCode(phone));
                }
                else rspObj.put("error",checkPhoneStatus);
                break;
            case 2:
                checkPhoneStatus=checkPhoneExist(phone);
                if(checkPhoneStatus==4){
                    rspObj.put("error",sendVerifyCode(phone));
                }
                else rspObj.put("error",checkPhoneStatus);
                break;
            case 3:
                rspObj.put("error",checkUserNameExist(userName));
                break;
            case 4:
                checkVerifyCodeStatus=checkVerifyCode(phone,verifyCode);
                if(checkVerifyCodeStatus==5) rspObj.put("error",5);
                else{
                    checkUserNameStatus=checkUserNameExist(userName);
                    if(checkUserNameStatus==1) rspObj.put("error",1);
                    else if(checkPhoneExist(phone)==2) rspObj.put("error",2);
                    else if (registerNewUser(userName,phone,password)!=0) rspObj.put("error",11);
                }
                break;
            case 5:
                checkPhoneStatus=checkPhoneExist(phone);
                if(checkPhoneStatus!=2) rspObj.put("error",checkPhoneStatus);
                else {
                    checkVerifyCodeStatus=checkVerifyCode(phone,verifyCode);
                    if(checkVerifyCodeStatus!=9) rspObj.put("error",checkVerifyCodeStatus);
                    else rspObj.put("error",resetPwd(phone,password));
                }
        }

        PrintWriter pw=rsp.getWriter();
        pw.write(rspObj.toJSONString());
        pw.flush();
    }

    protected void doOptions(HttpServletRequest req,HttpServletResponse rsp){
        Util.allowCrossOrigin(rsp);
    }

    public int checkUserNameExist(String userName){
        /**
         * 返回值
         * 8，用户名不存在
         * 1，用户名已经存在
         * 11，服务器操作数据库时出错
         */
        MysqlQuery q=new MysqlQuery();
        q.selectFromDb("select * from user where userName='" + userName + "'");
        ResultSet rs=q.rs;
        try{
            if(rs.next()){
                return 1;
            }
        }catch (Exception e){
            System.out.println(e);
            return 11;
        }
        q.closeDbConnection();
        return 8;
    }

    public int checkPhoneExist(String phone){
        /**
         * 返回值
         * 4,手机号未注册
         * 2，手机号已经注册
         */
        MysqlQuery q=new MysqlQuery();
        q.selectFromDb("select * from user where phone='" + phone + "'");
        ResultSet rs=q.rs;
        try{
            if(rs.next()){
                return 2;
            }
        }catch (Exception e){
            System.out.println(e);
            return 11;
        }
        q.closeDbConnection();
        return 4;
    }

    public int checkVerifyCode(String phone,String verifyCode){
        /**
         * 返回值
         * 5，校验码错误
         * 9，校验码正确
         * 11，服务器操作数据库错误
         */
        MysqlQuery q=new MysqlQuery();
        String sqlStr="select * from verify_code where phone='"+phone+"' order by time DESC";
        q.selectFromDb(sqlStr);
        try{
            if(!q.rs.next() || !q.rs.getString("verifyCode").equals(verifyCode)){
                return 5;
            }
        }catch (Exception e){
            System.out.println(e);
            return 11;
        }
        return 9;
    }

    public int sendVerifyCode(String phone){
        /**
         * 返回值
         * 6，发送验证码成功
         * 7，发送验证码失败
         */
        String generatedCode="";
        Random random=new Random();
        for(int i=0;i<6;i++){
            generatedCode+= random.nextInt(10);
        }

        int sendResult=HttpSender.post("http://jmsms.market.alicloudapi.com/sms/send?mobile="+phone+"&templateId=M72CB42894&value="+generatedCode,"");
        if(sendResult!=200) return 7;

        MysqlQuery q=new MysqlQuery();
        String sqlStr="insert into verify_code values ('"+generatedCode+"','"+phone+"','"+new Date().getTime()+"')";
        q.updateDbTable(sqlStr);
        if(q.rows<=0) return 11;
        q.closeDbConnection();
        return 6;
    }

    public int registerNewUser(String userName,String phone,String password){
        /**
         * 返回值
         * 0，注册成功
         */
        String generatedUid="";
        Random random=new Random();
        for(int i=0;i<10;i++){
            generatedUid+= random.nextInt(10);
        }
        MysqlQuery q=new MysqlQuery();
        String sqlStr="insert into user values (null,'"+generatedUid+"','"+userName+"','"+phone+"','"+password+"')";
        q.updateDbTable(sqlStr);
        if(q.rows<=0) return 11;
        return 0;
    }

    public int resetPwd(String phone,String password){
        /**
         * 返回值
         * 10，重置密码成功
         */
        MysqlQuery q=new MysqlQuery();
        String sqlStr="update user set password='"+password+"' where phone='"+phone+"'";
        q.updateDbTable(sqlStr);
        if(q.rows<=0) return 11;
        return 10;
    }
}
