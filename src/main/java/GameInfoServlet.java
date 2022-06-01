import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.FileItem;

import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet(urlPatterns = "/getGameInfo")
public class GameInfoServlet extends HttpServlet {

    // 上传配置
    private static final int MEMORY_THRESHOLD   = 1024 * 1024 * 3;  // 3MB
    private static final int MAX_FILE_SIZE      = 1024 * 1024 * 40; // 40MB
    private static final int MAX_REQUEST_SIZE   = 1024 * 1024 * 50; // 50MB
    String baseDir="";

    public void init(){
        ServletContext s1=this.getServletContext();
        baseDir=s1.getRealPath("/");
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        Util.allowCrossOrigin(rsp);
        JSONObject rspObj=new JSONObject();
        /**
         * 返回值
         * 1，获取游戏基本信息成功，基本信息包括：游戏名称、标签、简介、评分
         * -1，获取游戏基本信息失败
         * 2，游戏名称不存在
         * -2，游戏名称已存在
         * 3，上传游戏成功
         * -3，上传游戏失败
         * 4，获取用户上传游戏成功
         * -4，获取失败
         * 5，获取用户关注游戏成功
         * -5，获取失败
         * 6，获取游戏基本信息成功，基本信息参见下方注释
         * -6，获取失败
         */
        if(ServletFileUpload.isMultipartContent(req)){
            rspObj.put("error",uploadGame(req));
        }else {
            JSONObject reqPayload = JSON.parseObject(Util.parsePostPayload(req));
            int operType = reqPayload.getInteger("operType");
            String gameName = reqPayload.getString("gameName");
            String uid=reqPayload.getString("uid");
            String gid=reqPayload.getString("gid");
            /**
             * operType，操作类型
             * 1，上传新游戏
             * 2，校验游戏名是否存在
             * 3，获取用户上传过的游戏
             * 4，获取用户关注的游戏
             * 5，获取游戏基本信息，包括，视频、游戏名、上传用户名、上传时间、游戏介绍、评分、评分人数、标签
             */
            switch (operType) {
                case 1:
                    rspObj.put("error",uploadGame(req));
                    break;
                case 2:
                    rspObj.put("error", checkGameNameExist(gameName));
                    break;
                case 3:
                    int page=reqPayload.getInteger("page");
                    JSONArray userUploadGameJsonArr=new JSONArray();
                    JSONObject additionalObj=new JSONObject();
                    additionalObj.put("total",0);
                    rspObj.put("error",getUserUploadGame(reqPayload,userUploadGameJsonArr,additionalObj));
                    rspObj.put("userUploadGameList",userUploadGameJsonArr);
                    rspObj.put("total",additionalObj.getInteger("total"));
                    break;
                case 4:
                    page=reqPayload.getInteger("page");
                    JSONArray userStarGameJsonArr=new JSONArray();
                    additionalObj=new JSONObject();
                    additionalObj.put("total",0);
                    rspObj.put("error",getUserStarGame(reqPayload,userStarGameJsonArr,additionalObj));
                    rspObj.put("userStarGameList",userStarGameJsonArr);
                    rspObj.put("total",additionalObj.getInteger("total"));
                    break;
                case 5:
                    MysqlQuery q=new MysqlQuery();
                    JSONObject gameObj=new JSONObject();
                    int result=q.getGameBaseInfo(gid,gameObj);
                    rspObj.put("error",result==0? 6:-6);
                    rspObj.put("gameObj",gameObj);
                    break;
            }
        }

        rsp.setContentType("application/json;charset=utf-8");
        PrintWriter pw=rsp.getWriter();
        pw.write(rspObj.toJSONString());
        pw.flush();
    }

    public int checkGameNameExist(String gameName){
        int result;
        /**
         * 2，不存在
         * -2，已存在
         */
        MysqlQuery q=new MysqlQuery();
        String sqlStr="select gameName from game where gameName='"+gameName+"'";
        q.selectFromDb(sqlStr);
        try{
            if(q.rs.next()) result=-2;
            else result=2;
        }catch (Exception e){
            e.printStackTrace();
            result=11;
        }
        q.closeDbConnection();
        return  result;
    }

    public int uploadGame(HttpServletRequest request){
        int result=-3;
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(MEMORY_THRESHOLD);
        // 设置临时存储目录
        factory.setRepository(new File(System.getProperty("java.io.tmpdir")));

        ServletFileUpload upload = new ServletFileUpload(factory);
        // 设置最大文件上传值
        upload.setFileSizeMax(MAX_FILE_SIZE);
        // 设置最大请求值 (包含文件和表单数据)
        upload.setSizeMax(MAX_REQUEST_SIZE);

        String generatedGid=Util.getGeneratedId(10);
        // 文件存放路径
        String coverDirStr = baseDir + "\\resource\\game\\"+generatedGid;
        File coverDirFile=new File(coverDirStr);
        if(!coverDirFile.exists()) coverDirFile.mkdir();

        try {
            // 解析请求的内容提取文件数据
            List<FileItem> formItems = upload.parseRequest(request);
            JSONObject gameInfoObj=new JSONObject();
            String gameCoverUrl="";
            if (formItems != null && formItems.size() > 0) {
                for (FileItem item : formItems) {
                    // 处理不在表单中的字段
                    if (!item.isFormField()) {
                        String[] fileType=item.getContentType().split("/");
                        String fileName;
                        if(fileType[0].equals("image")) {
                            fileName="cover."+fileType[1];
                            gameCoverUrl=fileName;
                        }
                        else fileName="video.mp4";
                        String filePath = coverDirStr + File.separator + fileName;
                        File storeFile = new File(filePath);
                        // 在控制台输出文件的上传路径
                        System.out.println(filePath);
                        // 保存文件到硬盘
                        item.write(storeFile);
                    }else{
                        gameInfoObj.put(item.getFieldName(),new String(item.getString("utf-8"))); //按utf-8读取报文体中表单内容分，避免中文乱码
                    }
                }
            }
            MysqlQuery q=new MysqlQuery();
            String gameName=gameInfoObj.getString("gameName");
            String gameLabel=gameInfoObj.getString("gameLabel");
            String gameUploaderUid=gameInfoObj.getString("gameUploaderUid");
            String gameDesc=gameInfoObj.getString("gameDesc");
            String gameUploadTimeStamp=gameInfoObj.getString("gameUploadTimeStamp");
            String sqlStr="insert into game values('"+generatedGid+"','"+gameName+"','0',0,'"
                    +gameLabel+"','"+gameUploaderUid+"','"+gameCoverUrl+"','"+gameDesc+"','"+gameUploadTimeStamp+"')";
            q.updateDbTable(sqlStr);
            result=q.rows>0? 3:-3;
            q.closeDbConnection();
        } catch (Exception ex) {ex.printStackTrace();result=11;}
        return result;
    }

    public int  getUserUploadGame(JSONObject reqPayload,JSONArray userUploadGameJsonArr,JSONObject additionalObj){
        int result=-4;
        MysqlQuery q=new MysqlQuery();
        int page=reqPayload.getInteger("page");
        String filterGameName=reqPayload.getString("filterGameName");
        if(filterGameName==null) filterGameName="";
        String uid=reqPayload.getString("uid");
        String limitStart=""+(page-1)*10;
        String sqlStr="select gid,gameCoverUrl,gameName,gameDesc,gameRate,gameRaterNum from game where gameUploaderUid='"
                +uid+"' and gameName like '%"+filterGameName+"%' order by gameUploadTimeStamp DESC limit "+limitStart+",10";
        q.selectFromDb(sqlStr);
        try{
            while(q.rs.next()){
                JSONObject tmpObj=new JSONObject();
                tmpObj.put("gameCoverUrl",q.rs.getString("gameCoverUrl"));
                tmpObj.put("gameName",q.rs.getString("gameName"));
                tmpObj.put("gameDesc",q.rs.getString("gameDesc"));
                tmpObj.put("gameRate",q.rs.getString("gameRate"));
                tmpObj.put("gameRaterNum",q.rs.getInt("gameRaterNum"));
                tmpObj.put("gid",q.rs.getString("gid"));
                userUploadGameJsonArr.add(tmpObj);
            }
            sqlStr="select count(*) from game where gameUploaderUid='"+uid+"'";
            q.selectFromDb(sqlStr);
            q.rs.next();
            additionalObj.put("total",q.rs.getInt("count(*)"));
            result=4;
        }catch (Exception e){
            e.printStackTrace();
            result=11;
        }
        q.closeDbConnection();
        return result;
    }

    public int  getUserStarGame(JSONObject reqPayload,JSONArray userStarGameJsonArr,JSONObject additionalObj){
        int result=-5;
        MysqlQuery q=new MysqlQuery();
        int page=reqPayload.getInteger("page");
        String filterGameName=reqPayload.getString("filterGameName");
        if(filterGameName==null) filterGameName="";
        String uid=reqPayload.getString("uid");
        String limitStart=""+(page-1)*10;
        String sqlStr="select * from game where gid in (select gid from follow_game where followerUid='"
                +uid+"') and gameName like '%"+filterGameName+"%' order by gameUploadTimeStamp DESC limit "+limitStart+",10";
        q.selectFromDb(sqlStr);
        try{
            while(q.rs.next()){
                JSONObject tmpObj=new JSONObject();
                tmpObj.put("gameCoverUrl",q.rs.getString("gameCoverUrl"));
                tmpObj.put("gameName",q.rs.getString("gameName"));
                tmpObj.put("gameDesc",q.rs.getString("gameDesc"));
                tmpObj.put("gameRate",q.rs.getString("gameRate"));
                tmpObj.put("gameRaterNum",q.rs.getInt("gameRaterNum"));
                tmpObj.put("gid",q.rs.getString("gid"));
                userStarGameJsonArr.add(tmpObj);
            }
            sqlStr="select count(*) from game where gid in (select gid from follow_game where followerUid='"+uid+"')";
            q.selectFromDb(sqlStr);
            q.rs.next();
            additionalObj.put("total",q.rs.getInt("count(*)"));
            result=5;
        }catch (Exception e){
            e.printStackTrace();
            result=11;
        }
        q.closeDbConnection();
        return result;
    }

    protected void doOptions(HttpServletRequest req,HttpServletResponse rsp){Util.allowCrossOrigin(rsp);}
}
