import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Base64;

@WebServlet(urlPatterns = "/getUserInfo")
public class UserInfoServlet extends HttpServlet {
    String baseDir;

    public void init(){
        ServletContext s1=this.getServletContext();
        baseDir=s1.getRealPath("/");
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        Util.allowCrossOrigin(rsp);
        JSONObject rspObj=new JSONObject();
        /**
         * HTTP返回报文体
         * 1，获取用户基本信息成功，用户基本信息包括：昵称、手机号、性别、生日、个性签名
         * -1，获取用户基本信息失败
         * 2，获取用户头像成功
         * -2，获取用户头像失败（永远用不到，因为获取失败会返回默认头像url）
         * 3，设置用户基本信息成功
         * -3，设置用户基本信息失败，名称已存在
         * 4，设置用户头像成功
         * -4，设置用户头像失败
         */
        JSONObject reqPayload= JSON.parseObject( Util.parsePostPayload(req));
        String uid=reqPayload.getString("uid");
        String birthday=reqPayload.getString("birthday");
        String userName=reqPayload.getString("userName");
        String gender=reqPayload.getString("gender");
        String signature=reqPayload.getString("signature");
        String avatarFileBase64=reqPayload.getString("avatarFileBase64");
        int operType=reqPayload.getInteger("operType");
        /**
         * operType，请求类型
         * 1，获取用户个人基本信息（名称、性别、个性签名、手机号、头像）
         * 2，单独获取用户头像
         * 3，设置用户个人基本信息
         * 4，设置用户头像
         */
        switch (operType){
            case 1:
                MysqlQuery q=new MysqlQuery();
                JSONObject userBaseInfoObj=new JSONObject();
                int result=q.getUserBaseInfo(uid,userBaseInfoObj);
                rspObj.put("error",result==0? 1:-1);
                rspObj.put("userBaseInfo",userBaseInfoObj);
                break;
            case 2:
                String avatarUrl=getUserAvatarUrl(uid);
                rspObj.put("error",2);
                rspObj.put("avatarUrl",avatarUrl); //如果avatarUrl是null，那么前端收到的body里面没有avatarUrl这个字段
                break;
            case 3:
                rspObj.put("error",setUserBaseInfo(uid,birthday,userName,gender,signature));
                break;
            case 4:
                rspObj.put("error",setUserAvatar(uid,avatarFileBase64));
                break;
        }


        rsp.setContentType("application/json;charset=utf-8");
        PrintWriter pw=rsp.getWriter();
        pw.write(rspObj.toJSONString());
        pw.flush();
    }

    public String getUserAvatarUrl(String uid){
        String result=null;
        MysqlQuery q=new MysqlQuery();
        String sqlStr="select avatarUrl from user where uid='"+uid+"'";
        q.selectFromDb(sqlStr);
        try {
            if (q.rs.next()) {
                String avatarUrl = q.rs.getString("avatarUrl");
                if (avatarUrl != null) result = avatarUrl;
            }else result=null;
        }catch (Exception e) {System.out.println(e);}
        q.closeDbConnection();
        return  result;
    }

    public int setUserBaseInfo(String uid,String birthday,String userName,String gender,String signature){
        int result=-3;
        MysqlQuery q=new MysqlQuery();
        String sqlStr="update user set birthday='"+birthday+"', userName='"+userName+"',gender='"
                +gender+"',signature='"+signature+"' where uid='"+uid+"'";
        q.updateDbTable(sqlStr);
        if(q.rows>0) result=3;
        q.closeDbConnection();
        return  result;
    }

    public int setUserAvatar(String uid, String avatarFileBase64){
        int result;
        int position=avatarFileBase64.indexOf(",");
        byte[] bytes=Base64.getDecoder().decode(avatarFileBase64.substring(position+1));
        position=avatarFileBase64.indexOf(":");
        int position2=avatarFileBase64.indexOf(";");
        String fileTypeStr=avatarFileBase64.substring(position+1,position2);
        String fileSuffix=fileTypeStr.split("/")[1];

        try {
            String userDirStr=baseDir + "\\resource\\user\\"+ uid;
            File userDirFile=new File(userDirStr);
            if(!userDirFile.exists()) userDirFile.mkdir();
            FileOutputStream fops = new FileOutputStream( userDirStr+ "\\avatar." + fileSuffix );
            fops.write(bytes);
            fops.flush();
            String pathInDb="avatar." + fileSuffix;
            MysqlQuery q=new MysqlQuery();
            String sqlStr="update user set avatarUrl='"+pathInDb+"' where uid='"+uid+"'";
            q.updateDbTable(sqlStr);
            result= q.rows>0? 4:-4;
            q.closeDbConnection();
        }catch (Exception e){
            e.printStackTrace();
            result=-4;
        }
        return result;
    }

    protected void doOptions(HttpServletRequest req,HttpServletResponse rsp){
        Util.allowCrossOrigin(rsp);
    }
}
