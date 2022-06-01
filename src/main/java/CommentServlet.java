import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@WebServlet(urlPatterns = "/getComment")
public class CommentServlet extends HttpServlet {

    protected void doPost(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        Util.allowCrossOrigin(rsp);
        JSONObject rspObj=new JSONObject();
        /**
         * 返回报文体
         * 1，获取评论成功
         * -1，获取失败
         * 2，添加评论成功
         * -2，添加失败
         * 3，（取消）赞、（取消）踩操作成功
         * -3，操作失败
         */
        JSONObject reqPayload= JSON.parseObject( Util.parsePostPayload(req));
        int operType=reqPayload.getInteger("operType");
        /**
         * 操作类型
         * 1，获取游戏评论
         * 2，添加游戏评论
         * 3，（取消）赞、（取消）踩
         */
        switch(operType){
            case 1:
                String gid=reqPayload.getString("gid");
                int page=reqPayload.getInteger("page");
                String uid=reqPayload.getString("uid");
                Set<String> uidSet=new HashSet<>();
                JSONArray commentListJsonArr=new JSONArray();
                JSONObject additionalObj=new JSONObject();
                int result=getComment(gid,page,uidSet,commentListJsonArr,additionalObj);
                if(result==1){
                    result=getCommentUserInfo(uidSet,commentListJsonArr);
                    if(result!=0) rspObj.put("error",-1);
                    else {
                        rspObj.put("error", getCommentLiked(uid,commentListJsonArr)== 0 ? 1 : -1);
                        rspObj.put("commentList", commentListJsonArr);
                        rspObj.put("total",additionalObj.getInteger("total"));
                    }
                }else rspObj.put("error",result);
                break;
            case 2:
                rspObj.put("error",addComment(reqPayload));
                break;
            case 3:
                rspObj.put("error",setLikeStatus(reqPayload));
                break;
        }
        rsp.setContentType("application/json;charset=utf-8");
        PrintWriter pw=rsp.getWriter();
        pw.write(rspObj.toJSONString());
        pw.flush();
    }

    public int getComment(String gid,int page, Set<String> uidSet, JSONArray commentListJsonArr,JSONObject additionalObj){
        int result=-1;
        /**
         * 1，获取评论成功
         * -1，获取评论失败
         * 11，服务器运行失败
         */
        MysqlQuery q=new MysqlQuery();
        String limitStart=""+(page-1)*10;
        String sqlStr="select * from game_comment where gid='"+gid+"' order by timeStamp DESC limit "+limitStart+",10";
        q.selectFromDb(sqlStr);
        try{
            while (q.rs.next()){
                int cid=q.rs.getInt("id");
                String uid=q.rs.getString("uid");
                String content=q.rs.getString("content");
                int likeNum=q.rs.getInt("likeNum");
                int dislikeNum=q.rs.getInt("dislikeNum");
                String timeStamp=q.rs.getString("timeStamp");
                uidSet.add(uid);
                JSONObject tmpObj=new JSONObject();
                JSONObject publisherObj=new JSONObject();
                publisherObj.put("uid",uid);
                publisherObj.put("timeStamp",timeStamp);
                tmpObj.put("cid",cid);
                tmpObj.put("publisher",publisherObj);
                tmpObj.put("content",content);
                tmpObj.put("likeNum",likeNum);
                tmpObj.put("dislikeNum",dislikeNum);
                commentListJsonArr.add(tmpObj);
            }
            sqlStr="select count(*) from game_comment where gid='"+gid+"'";
            q.selectFromDb(sqlStr);
            if(q.rs.next()){
                additionalObj.put("total",q.rs.getInt("count(*)"));
                result=1;
            }else result=-1;
        }catch (Exception e){
            e.printStackTrace();
            result=11;
        }
        q.closeDbConnection();
        return result;
    }

    public int getCommentUserInfo(Set<String> uidSet,JSONArray commentListJsonArr){
        int result=0;
        /**
         * 同new MysqlQuery.getUserBaseInfo的返回值
         */
        JSONObject uidMap=new JSONObject();
        for(Iterator<String> it= uidSet.iterator(); it.hasNext();){
            MysqlQuery q=new MysqlQuery();
            JSONObject tmpObj=new JSONObject();
            String uid=it.next();
            result=q.getUserBaseInfo(uid,tmpObj);
            if(result!=0) return result;
            uidMap.put(uid,tmpObj);
        }
        for(int i=0;i<commentListJsonArr.size();i++){
            JSONObject commentObj=commentListJsonArr.getJSONObject(i);
            JSONObject publisherObj=commentObj.getJSONObject("publisher");
            String uid=publisherObj.getString("uid");
            JSONObject userObj=uidMap.getJSONObject(uid);
            publisherObj.put("userName",userObj.getString("userName"));
            publisherObj.put("avatarUrl",userObj.getString("avatarUrl"));
        }
        return result;
    }

    public int getCommentLiked(String uid,JSONArray commentListJsonArr){
        int result=0;
        /**
         * 0，没有错误
         * 11，解析数据失败
         */
        MysqlQuery q=new MysqlQuery();
        String sqlStr;
        for(int i=0;i<commentListJsonArr.size();i++){
            JSONObject commentObj=commentListJsonArr.getJSONObject(i);
            int cid=commentObj.getInteger("cid");
            sqlStr="select * from like_comment where uid='"+uid+"' and cid="+cid;
            q.selectFromDb(sqlStr);
            try {
                commentObj.put("liked",q.rs.next()? q.rs.getInt("likeStatus") : 0);
            }catch (Exception e){
                System.out.println("解析like_comment数据失败");
                e.printStackTrace();
                result=11;
                break;
            }
        }
        q.closeDbConnection();
        return result;
    }

    public int addComment(JSONObject reqPayload){
        int result;
        String uid=reqPayload.getString("uid");
        String gid=reqPayload.getString("gid");
        String content=reqPayload.getString("content");
        String timeStamp=reqPayload.getString("timeStamp");
        MysqlQuery q=new MysqlQuery();
        String sqlStr="insert into game_comment values(null,'"+uid+"','"+gid+"','"+content+"',0,0,'"+timeStamp+"')";
        q.updateDbTable(sqlStr);
        result=q.rows>0? 2:-2;
        q.closeDbConnection();
        return result;
    }

    public int setLikeStatus(JSONObject reqPayload){
        int result=-3;
        String uid=reqPayload.getString("uid");
        String gid=reqPayload.getString("gid");
        int cid=reqPayload.getInteger("cid");
        int newLike=reqPayload.getInteger("newLike");
        int oldLike=reqPayload.getInteger("oldLike");
        int type=1;
        /**
         * type，判断具体操作
         * 1，点赞
         * -1，取消点赞
         * 2，踩
         * -2，取消踩
         * 3，取消踩并点赞
         * -3，取消赞并踩
         */
        MysqlQuery q=new MysqlQuery();
        String sqlStr="update like_comment set likeStatus="+newLike+" where uid='"+uid+"' and cid="+cid;
        if(oldLike==0) {
            type= newLike==1?1:2;
            sqlStr="insert into like_comment values(null,'"+uid+"',"+cid+","+newLike+")";
        }else{
            if(newLike==0) {
                sqlStr="delete from like_comment where uid='"+uid+"' and cid="+cid;
                type= oldLike==1?-1:-2;
            }else {
                type = newLike == 1 ? 3 : -3;
            }
        }
        q.updateDbTable(sqlStr);
        if(q.rows<=0) result=-3;
        else {
            sqlStr = "select likeNum,dislikeNum from game_comment where id=" + cid;
            q.selectFromDb(sqlStr);
            try {
                if (q.rs.next()) {
                    int currentLikeNum=q.rs.getInt("likeNum");
                    int currentDislikeNum=q.rs.getInt("dislikeNum");
                    int newLikeNum=currentLikeNum;
                    int newDislikeNum=currentDislikeNum;
                    switch (type){
                        case 1:
                            newLikeNum+=1;
                            break;
                        case -1:
                            newLikeNum-=1;
                            break;
                        case 2:
                            newDislikeNum+=1;
                            break;
                        case -2:
                            newDislikeNum-=1;
                            break;
                        case 3:
                            newDislikeNum-=1;
                            newLikeNum+=1;
                            break;
                        case -3:
                            newDislikeNum+=1;
                            newLikeNum-=1;
                            break;
                    }
                    sqlStr="update game_comment set likeNum="+newLikeNum+",dislikeNum="+newDislikeNum+" where id="+cid;
                    q.updateDbTable(sqlStr);
                    result=q.rows>0?3:-3;
                }else result=-3;
            } catch (Exception e) {
                System.out.println("解析game_comment数据失败");
                e.printStackTrace();
                result= 11;
            }
        }
        q.closeDbConnection();
        return result;
    }

    protected void doOptions(HttpServletRequest req,HttpServletResponse rsp){
        Util.allowCrossOrigin(rsp);
    }
}
