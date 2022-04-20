import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.*;

public class Util {
    static String dbUrl="jdbc:mysql://116.205.171.68:3306/hh_gamer";
    static String dbUserName="hh";
    static String dbPwd="722248hh";

    public static String parsePostPayload(HttpServletRequest req) throws UnsupportedEncodingException {
        req.setCharacterEncoding("utf-8");
        String result=null;
        StringBuilder sb=new StringBuilder();
        try(BufferedReader reader=req.getReader();){
            char[] buff=new char[1024];
            int len;
            while((len=reader.read(buff))!=-1){
                sb.append(buff,0,len);
            }
            result=sb.toString();
        }catch (IOException e){
            System.out.println(e);
        }
        return result;
    }

    public static void allowCrossOrigin(HttpServletResponse response){
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:8080");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "*");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "Authorization,Origin,X-Requested-With,Content-Type,Accept,"
                + "content-Type,origin,x-requested-with,content-type,accept,authorization,token,id,X-Custom-Header,X-Cookie,Connection,User-Agent,Cookie,*");
        response.setHeader("Access-Control-Request-Headers", "Authorization,Origin, X-Requested-With,content-Type,Accept");
        response.setHeader("Access-Control-Expose-Headers", "*");
    }

    public static Cookie setCookie(String keyName,String value,String path,int maxAge){
        Cookie cookie=new Cookie(keyName,value);
        cookie.setPath("/");
        // 该Cookie有效期:
        cookie.setMaxAge(maxAge);// 单位是秒
        //cookie.setSecure(true); //如果访问的是https网页需要设置这一步
        return cookie;
    }
}
