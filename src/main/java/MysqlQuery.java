import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.sql.*;

public class MysqlQuery {
    static String dbUrl="jdbc:mysql://116.205.171.68:3306/hh_gamer";
    static String dbUserName="hh";
    static String dbPwd="722248hh";

    public Connection conn;
    public Statement stat;
    public ResultSet rs;
    public int rows=-1;
    public int error=0;
    /**
     * 错误码
     * 0，没有错误
     * 1，驱动未找到
     * 2，创建连接失败
     * 3，执行查询失败
     * 4，执行更新失败
     * 5，关闭数据库连接失败
     */

    public MysqlQuery(){
        System.out.println("记得关闭数据库连接");
        try {
            Class.forName("com.mysql.jdbc.Driver");
        }catch(ClassNotFoundException e1){
            error=1;
            System.out.println("驱动程序未找到");
        }
        try {
            conn = DriverManager.getConnection(dbUrl, dbUserName, dbPwd);
            stat = conn.createStatement();
        }catch (Exception e){
            error=2;
            System.out.println("创建数据库连接失败");
            e.printStackTrace();
        }
    }

    public void selectFromDb(String sqlStr){
        if(stat==null) return;
        try {
            rs = stat.executeQuery(sqlStr);
        }catch (Exception e){
            e.printStackTrace();
            error=3;
            System.out.println("执行下列sql语句失败");
            System.out.println(sqlStr);
        }
    }

    public void updateDbTable(String sqlStr){
       if(stat==null) return;
        try {
            rows = stat.executeUpdate(sqlStr);
        }catch (Exception e){
            e.printStackTrace();
            error=4;
            System.out.println("执行下列sql语句失败");
            System.out.println(sqlStr);
        }
    }

    public void closeDbConnection(){
        try {
            if (rs != null) rs.close();
            if (stat != null) stat.close();
            if (conn != null) conn.close();
        }catch (Exception e){
            error=5;
            System.out.println("关闭数据库连接失败");
            e.printStackTrace();
        }
    }



    public int getUserBaseInfo(String uid,JSONObject userObj){
        int result=0;
        /**
         * 0，没有任何问题
         * 1，查询数据库时出错
         * 2，解析数据库结果时出错
         */
        String sqlStr="select * from user where uid='"+uid+"'";
        selectFromDb(sqlStr);
        if(error!=0)  {result=1;return result;}
        try{
            if(rs.next()) {
                userObj.put("userName", rs.getString("userName"));
                userObj.put("phone", rs.getString("phone"));
                userObj.put("gender", rs.getString("gender"));
                userObj.put("signature", rs.getString("signature"));
                userObj.put("birthday", rs.getString("birthday"));
                userObj.put("avatarUrl",rs.getString("avatarUrl"));
            }else result=2;
        }catch (Exception e){
            result=2;
            System.out.println("解析user数据失败");
            e.printStackTrace();
        }
        closeDbConnection();
        return result;
    }


    public int getGameBaseInfo(String gid,JSONObject gameObj){
        int result=0;
        /**
         * 0，没有任何问题
         * 1，查询数据库时出错
         * 2，解析数据库结果时出错
         */
        String sqlStr="select * from game where gid='"+gid+"'";
        selectFromDb(sqlStr);
        if(error!=0)  {result=1;return result;}
        try{
            if(rs.next()){
                gameObj.put("gameName",rs.getString("gameName"));
                gameObj.put("gameRate",rs.getString("gameRate"));
                gameObj.put("gameRaterNum",rs.getInt("gameRaterNum"));
                String[] gameLabelList=rs.getString("gameLabel").split(",");
                JSONArray tmpArr=new JSONArray();
                if(!gameLabelList[0].equals("")) {
                    for (String item : gameLabelList) {
                        tmpArr.add(item);
                    }
                }
                gameObj.put("gameLabelList",tmpArr);
                String gameUploaderUid=rs.getString("gameUploaderUid");
                gameObj.put("gameDesc",rs.getString("gameDesc"));
                gameObj.put("gameUploadTimeStamp",rs.getString("gameUploadTimeStamp"));
                sqlStr="select userName from user where uid='"+gameUploaderUid+"'";
                selectFromDb(sqlStr);
                rs.next();
                gameObj.put("gameUploaderUid",gameUploaderUid);
                gameObj.put("gameUploaderUserName",rs.getString("userName"));
            }else result=2;
        }catch (Exception e){
            result=2;
            System.out.println("解析game或user数据失败");
            e.printStackTrace();
        }
        closeDbConnection();
        return result;
    }
}
