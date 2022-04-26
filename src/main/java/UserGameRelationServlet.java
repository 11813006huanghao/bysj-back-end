import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(urlPatterns = "/getUserGameRelation")
public class UserGameRelationServlet extends HttpServlet {

    protected void doPost(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        Util.allowCrossOrigin(rsp);
        JSONObject rspObj=new JSONObject();
        rspObj.put("error",-1);
        /**
         * 1，获取用户和游戏关系成功
         * -1，获取失败
         * 2，关注/取消关注游戏成功
         * -2，失败
         * 3，给游戏评分成功
         * -3，评分失败
         */
        JSONObject reqPayload= JSON.parseObject( Util.parsePostPayload(req));
        String uid=reqPayload.getString("uid");
        String gid=reqPayload.getString("gid");
        int operType=reqPayload.getInteger("operType");
        /**
         * 操作类型
         * 1，获取用户和游戏关系，包括，是否关注，是否评分
         * 2，关注/取消关注游戏
         * 3，给游戏评分
         */
        switch (operType){
            case 1:
                MysqlQuery q=new MysqlQuery();
                String sqlStr="select * from follow_game where followerUid='"+uid+"' and gid='"+gid+"'";
                q.selectFromDb(sqlStr);
                try {
                    rspObj.put("isFollowed",q.rs.next());
                    sqlStr="select * from rate_game where raterUid='"+uid+"' and gid='"+gid+"'";
                    q.selectFromDb(sqlStr);
                    rspObj.put("rate",q.rs.next()? q.rs.getString("rate"):"-1");
                    rspObj.put("error",1);
                }catch (Exception e){
                    e.printStackTrace();
                    rspObj.put("error",11);
                }
                q.closeDbConnection();
                break;
            case 2:
                Boolean hasStarGame=reqPayload.getBoolean("hasStarGame");
                q=new MysqlQuery();
                sqlStr= hasStarGame? "delete from follow_game where followerUid='"+uid+"' and gid='"+gid+"'" :
                        "insert into follow_game values(null,'"+uid+"','"+gid+"')";
                q.updateDbTable(sqlStr);
                rspObj.put("error",q.rows>0 ? 2:-2);
                q.closeDbConnection();
                break;
            case 3:
                String rate=reqPayload.getString("rate");
                q=new MysqlQuery();
                sqlStr="insert into rate_game values(null,'"+uid+"','"+gid+"','"+rate+"')";
                q.updateDbTable(sqlStr);
                if(q.rows>0){
                    try {
                        sqlStr = "select gameRate,gameRaterNum from game where gid='" + gid + "'";
                        q.selectFromDb(sqlStr);
                        q.rs.next();
                        double currentGameRate=Double.parseDouble(q.rs.getString("gameRate"));
                        int currentRaterNum=q.rs.getInt("gameRaterNum");
                        double newGameRate=(currentGameRate*currentRaterNum+Double.parseDouble(rate))/(currentRaterNum+1);
                        sqlStr="update game set gameRate='"+String.format("%.1f",newGameRate)+"', gameRaterNum="
                                +(currentRaterNum+1)+" where gid='"+gid+"'";
                        q.updateDbTable(sqlStr);
                        rspObj.put("error",q.rows>0? 3:-3);
                    }catch (Exception e){
                        e.printStackTrace();
                        rspObj.put("error",11);
                    }
                }else rspObj.put("error",-3);
                q.closeDbConnection();
                break;
        }


        rsp.setContentType("application/json");
        PrintWriter pw=rsp.getWriter();
        pw.write(rspObj.toJSONString());
        pw.flush();
    }

    protected void doOptions(HttpServletRequest req,HttpServletResponse rsp){
        Util.allowCrossOrigin(rsp);
    }
}
