import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(urlPatterns = "/getUserRelation")
public class UserRelationServlet extends HttpServlet {

    protected void doPost(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        Util.allowCrossOrigin(rsp);
        JSONObject rspObj=new JSONObject();
        /**
         * 响应报文体
         * 1，用户A关注了用户B
         * -1，没有关注
         * 2，获取用户A所有关注列表成功
         * -2，获取失败
         * 4，用户A（取消）关注用户B成功
         * -4，失败
         */
        JSONObject reqPayload= JSON.parseObject( Util.parsePostPayload(req));
        int operType=reqPayload.getInteger("operType");
        /**
         * operType，操作类型，
         * 1，判断用户A是否关注用户B
         * 2，获取用户A的所有关注列表
         * 3，获取用户A的粉丝列表
         * 4，用户A（取消）关注用户B
         */
        switch (operType){
            case 1:
                String followerUid=reqPayload.getString("followerUid");
                String followedUid=reqPayload.getString("followedUid");
                MysqlQuery q=new MysqlQuery();
                String sqlStr="select * from follow_user where followerUid='"+followerUid+"' and followedUid='"+followedUid+"'";
                q.selectFromDb(sqlStr);
                try {
                    rspObj.put("error", q.rs.next() ? 1 : -1);
                }catch (Exception e){
                    e.printStackTrace();
                    rspObj.put("error",11);
                }
                q.closeDbConnection();
                break;
            case 2:
                JSONArray userListJsonArr=new JSONArray();
                int result=getFollowUserList(reqPayload,userListJsonArr,rspObj);
                if(result==0){
                    rspObj.put("error",2);
                    rspObj.put("userList",userListJsonArr);
                }else rspObj.put("error",-2);
                break;
            case 4:
                followerUid=reqPayload.getString("followerUid");
                followedUid=reqPayload.getString("followedUid");
                boolean hasStarUser=reqPayload.getBoolean("hasStarUser");
                sqlStr= hasStarUser? "delete from follow_user where followerUid='"+followerUid+"' and followedUid='"+followedUid+"'" :
                        "insert into follow_user values(null,'"+followerUid+"','"+followedUid+"')";
                q=new MysqlQuery();
                q.updateDbTable(sqlStr);
                rspObj.put("error",q.rows>0 ? 4:-4);
                q.closeDbConnection();
                break;
        }

        rsp.setContentType("application/json;charset=utf-8");
        PrintWriter pw=rsp.getWriter();
        pw.write(rspObj.toJSONString());
        pw.flush();
    }

    public int getFollowUserList(JSONObject reqPayload, JSONArray userListJsonArr,JSONObject rspObj){
        int result=0;
        /**
         * 0，一切正常
         * 1，获取用户基本信息失败
         */
        String followerUid=reqPayload.getString("followerUid");
        int page=reqPayload.getInteger("page");
        String filterUserName=reqPayload.getString("filterUserName");
        if(filterUserName==null) filterUserName="";
        String limitStart=""+(page-1)*1;
        MysqlQuery q=new MysqlQuery();
        String sqlStr="select count(*) from follow_user where followerUid='"+followerUid
                +"' and followedUid in (select uid from user where userName like '%"+filterUserName+"%')";
        q.selectFromDb(sqlStr);
        try{
            if(q.rs.next()){
                rspObj.put("total",q.rs.getInt("count(*)"));
            }
            sqlStr="select * from follow_user where followerUid='"+followerUid
                    +"' and followedUid in (select uid from user where userName like '%"+filterUserName+"%') limit "+limitStart+",1";
            q.selectFromDb(sqlStr);
            while (q.rs.next()){
                String followedUid=q.rs.getString("followedUid");
                JSONObject userObj=new JSONObject();
                userObj.put("uid",followedUid);
                MysqlQuery q2=new MysqlQuery();
                if(q2.getUserBaseInfo(followedUid,userObj)!=0){
                    result=1;
                    break;
                }
                q2=new MysqlQuery();
                sqlStr="select count(*) from follow_user where followerUid='"+followedUid+"'";
                q2.selectFromDb(sqlStr);
                if(q2.rs.next()){
                    userObj.put("followNum",q2.rs.getInt("count(*)"));
                }
                sqlStr="select count(*) from follow_user where followedUid='"+followedUid+"'";
                q2.selectFromDb(sqlStr);
                if(q2.rs.next()){
                    userObj.put("fansNum",q2.rs.getInt("count(*)"));
                }
                sqlStr="select count(*) from message where receiverUid='"+followedUid+"'";
                q2.selectFromDb(sqlStr);
                if(q2.rs.next()){
                    userObj.put("messageNum",q2.rs.getInt("count(*)"));
                }
                sqlStr="select count(*) from dynamic where uid='"+followedUid+"'";
                q2.selectFromDb(sqlStr);
                if(q2.rs.next()){
                    userObj.put("dynamicNum",q2.rs.getInt("count(*)"));
                }
                q2.closeDbConnection();
                userListJsonArr.add(userObj);
            }
        }catch (Exception e){
            e.printStackTrace();
            result=11;
        }
        q.closeDbConnection();
        return result;
    }

    protected void doOptions(HttpServletRequest req,HttpServletResponse rsp){
        Util.allowCrossOrigin(rsp);
    }

}
