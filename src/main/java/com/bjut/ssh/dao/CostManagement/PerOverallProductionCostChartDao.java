package com.bjut.ssh.dao.CostManagement;

import com.bjut.ssh.entity.Msg;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
//import java.time.Month;
import java.util.*;

/**
 * @Title: BasicOperatingExpensesDao
 * @Description: TODO
 * @Author: lxy
 * @CreateDate: 2018/7/6 13:55
 * @Version: 1.0
 */

@Repository
@Transactional
public class PerOverallProductionCostChartDao {

    @Autowired
    private SessionFactory sessionFactory;

    public Session getSession() {
        return this.sessionFactory.openSession();
    }

    public void close(Session session) {
        if (session != null)
            session.close();
    }

    //项目下拉菜单获取
    public Msg getItemSelect() {
        Session session = null;
        Transaction tx = null;
        //List<Departmentbase> DepartmentbaseList = null;
        String departmentCatNo;
        List<Map<String, Object>> list = new ArrayList<>();

        session = getSession();
        tx = session.beginTransaction();

        //普通科目
        Query query_getItemNo = session.createQuery("select graphData1 from Cbtbsz where graphNo = :a");
        query_getItemNo.setString("a", "000014");
        String ItemNo = (String) query_getItemNo.uniqueResult();
        String[] split_itemNo1 = ItemNo.split("\\;");

        for (String itemNo : split_itemNo1) {
            Query query = session.createQuery("select itemName from Lskmzd where itemNo = :a");
            query.setString("a", itemNo);
            String ItemName = (String) query.uniqueResult();
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("0", ItemName);//核算名称
            map.put("1", itemNo);//核算编号
            list.add(map);
        }

        //自定义科目
        Query query_getDefinedItemNo = session.createQuery("select graphData2 from Cbtbsz where graphNo = :a");
        query_getDefinedItemNo.setString("a", "000014");
        String definedItemNo = (String) query_getDefinedItemNo.uniqueResult();
        String[] split_itemNo = definedItemNo.split("\\;");

        for (String itemNo : split_itemNo) {
            Query query = session.createQuery("select itemName from Cbqtsz where itemNo = :a");
            query.setString("a", itemNo);
            String definedItemName = (String) query.uniqueResult();
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("0", definedItemName);//核算名称
            map.put("1", itemNo);//核算编号
            list.add(map);
        }

        if (list.size() > 0) {
            tx.commit();
            close(session);
            return Msg.success().add("PerOverallProductionCostChart_getItemSelect", list);
        } else {
            tx.commit();
            close(session);
            return Msg.fail().add("errorInfo", "编号重复，请重新输入");
        }
    }

    //获取图表的主题
    public Msg getEchartsTheme() {
        Session session = null;
        Transaction tx = null;
        session = getSession();
        tx = session.beginTransaction();

        List<String> themeList = new ArrayList<>();

        Query query = session.createQuery("select graphTheme from Cbtbsz where graphNo = :a");
        query.setString("a", "000014");
        String graphTheme = (String) query.uniqueResult();
        themeList.add(graphTheme);

        tx.commit();
        close(session);
        return Msg.success().add("PerOverallProductionCostChart_getEchartsTheme", themeList);
    }

    //获取图表数据(只是做了自定义字段)
    public Msg getValData1(String selectItemValue) {
        String sqlField_Summation = "debitMoney01";
        String sqlFiled_Production = "debitQty01";
        List<List<String>> Item_List = new ArrayList<>();//传回前端
        Calendar calendar = Calendar.getInstance();//可以对每个时间域单独修改
        int month = calendar.get(Calendar.MONTH);
        month++;
        List<String> itemCostList_Now = new ArrayList<>();
        List<String> itemCostList_Last = new ArrayList<>();
        //判断该科目是普通科目还是自定义字段
        String itemType = judgeTypeByItemNo(selectItemValue);
        //普通科目
        if (itemType.equals("1")) {
            //将本月之前的所有月份计算一遍
            for (int itemMonth = 1; itemMonth <= month; itemMonth++) {      //此循环是将本月之前的所有月份遍历一遍
                //将sqlField_Summation弄好
                String selectTime = String.valueOf(itemMonth);
                if (itemMonth < 10) {
                    selectTime = "0" + selectTime;
                }
                sqlField_Summation = "debitMoney" + selectTime;
                sqlFiled_Production = "debitQty" + selectTime;

                //今年
                //查询该月的金额
                Double itemValueNow = Double.parseDouble(getItemNoMoney(selectItemValue, "now", sqlField_Summation));
                //查询该月的产量
                Double itemProductionNow = getItemNoProduction(selectItemValue, "now", sqlFiled_Production);
                //计算相应的方气
                Double perItemCost_Now = (double) Math.round((calculateData(itemValueNow, itemProductionNow) * 100) / 100);
                String itemValueSum_Now = perItemCost_Now.toString();
                itemCostList_Now.add(itemValueSum_Now);
                //去年
                //查询该月的金额
                Double itemValueLast = Double.parseDouble(getItemNoMoney(selectItemValue, "last", sqlField_Summation));
                //查询该月的产量
                Double itemProductionLast = getItemNoProduction(selectItemValue, "last", sqlFiled_Production);
                //计算相应的方气
                Double perItemCost_Last = (double) Math.round((calculateData(itemValueLast, itemProductionLast) * 100) / 100);
                String itemValueSum_Last = perItemCost_Last.toString();
                itemCostList_Last.add(itemValueSum_Last);
            }
        }
        //自定义科目
        if (itemType.equals("2")) {
            //将本月之前的所有月份计算一遍
            for (int itemMonth = 1; itemMonth <= month; itemMonth++) {      //此循环是将本月之前的所有月份遍历一遍
                //将sqlField_Summation弄好
                String selectTime = String.valueOf(itemMonth);
                if (itemMonth < 10) {
                    selectTime = "0" + selectTime;
                }
                sqlField_Summation = "debitMoney" + selectTime;
                sqlFiled_Production = "debitQty" + selectTime;

                //今年
                //查询该月的金额
                Double itemValueNow = Double.parseDouble(getDefinedItemNoMoney(selectItemValue, "now", sqlField_Summation));
                //查询该月的产量
                Double itemProductionNow = getDefinedItemNoProduction(selectItemValue, "now", sqlFiled_Production);
                //计算相应的方气
                Double perItemCost_Now = (double) Math.round((calculateData(itemValueNow, itemProductionNow) * 100) / 100);
                String itemValueSum_Now = perItemCost_Now.toString();
                itemCostList_Now.add(itemValueSum_Now);
                //去年
                //查询该月的金额
                Double itemValueLast = Double.parseDouble(getDefinedItemNoMoney(selectItemValue, "last", sqlField_Summation));
                //查询该月的产量
                Double itemProductionLast = getDefinedItemNoProduction(selectItemValue, "last", sqlFiled_Production);
                //计算相应的方气
                Double perItemCost_Last = (double) Math.round((calculateData(itemValueLast, itemProductionLast) * 100) / 100);
                String itemValueSum_Last = perItemCost_Last.toString();
                itemCostList_Last.add(itemValueSum_Last);
            }
        }

        Item_List.add(itemCostList_Now);
        Item_List.add(itemCostList_Last);

        return Msg.success().add("PerOverallProductionCostChart_value1_List", Item_List);
    }

    //报表数据
    public Msg getDatagridData() {
        String sqlField_Summation = "debitMoney01";
        String sqlFiled_Production = "debitQty01";
        List<Map<String, Object>> Item_List = new ArrayList<>();//传回前端

        String itemNo1 = null;//所有科目字典的编号
        String[] split_itemNo1 = null;//科目编号数组
        String itemNo2 = null;//所有自定义科目的编号
        String[] split_itemNo2 = null;//自定义科目编号数组
        Calendar calendar = Calendar.getInstance();//可以对每个时间域单独修改
        int month = calendar.get(Calendar.MONTH);
        month++;
        Session session = null;
        Transaction tx = null;

        //普通科目
        //获取普通科目
        itemNo1 = getGraphData("1", "000014");
        split_itemNo1 = itemNo1.split("\\;");
        for (String itemNo : split_itemNo1) {
            String itemName = getItemName("1", itemNo);
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("text", itemName);
            //将本月之前的所有月份计算一遍
            for (int itemMonth = 1; itemMonth <= month; itemMonth++) {      //此循环是将本月之前的所有月份遍历一遍
                //将sqlField_Summation弄好
                String selectTime = String.valueOf(itemMonth);
                if (itemMonth < 10) {
                    selectTime = "0" + selectTime;
                }
                sqlField_Summation = "debitMoney" + selectTime;
                sqlFiled_Production = "debitQty" + selectTime;

                //查询该月的金额
                //查询该月的金额
                Double itemValueNow = Double.parseDouble(getItemNoMoney(itemNo, "now", sqlField_Summation));
                //查询该月的产量
                Double itemProductionNow = getItemNoProduction(itemNo, "now", sqlFiled_Production);
                //计算相应的方气
                Double perItemCost_Now = (double) Math.round((calculateData(itemValueNow, itemProductionNow) * 100) / 100);
                String itemValueSum_Now = perItemCost_Now.toString();
                String monthName = "month" + itemMonth;
                map.put(monthName, itemValueSum_Now);
            }
            Item_List.add(map);
        }

        //自定义字段
        //获取自定义字段
        itemNo2 = getGraphData("2", "000014");
        split_itemNo2 = itemNo2.split("\\;");
        for (String itemNo : split_itemNo2) {
            String itemName = getItemName("2", itemNo);
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("text", itemName);
            //将本月之前的所有月份计算一遍
            for (int itemMonth = 1; itemMonth <= month; itemMonth++) {      //此循环是将本月之前的所有月份遍历一遍
                //将sqlField_Summation弄好
                String selectTime = String.valueOf(itemMonth);
                if (itemMonth < 10) {
                    selectTime = "0" + selectTime;
                }
                sqlField_Summation = "debitMoney" + selectTime;
                sqlFiled_Production = "debitQty" + selectTime;

                //查询该月的金额
                Double itemValueNow = Double.parseDouble(getDefinedItemNoMoney(itemNo, "now", sqlField_Summation));
                //查询该月的产量
                Double itemProductionNow = getDefinedItemNoProduction(itemNo, "now", sqlFiled_Production);
                //计算相应的方气
                Double perItemCost_Now = (double) Math.round((calculateData(itemValueNow, itemProductionNow) * 100) / 100);
                String itemValueSum_Now = perItemCost_Now.toString();
                String monthName = "month" + itemMonth;
                map.put(monthName, itemValueSum_Now);
            }
            Item_List.add(map);
        }

        return Msg.success().add("PerOverallProductionCostChart_datagridValue_List", Item_List);
    }

    //获取编号的名称
    public String getItemName(String itemType, String itemNo) {
        Session session1 = null;
        Transaction tx1 = null;
        String result = null;

        try {
            session1 = getSession();
            tx1 = session1.beginTransaction();

            //普通科目
            if (itemType.equals("1")) {
                Query query = session1.createQuery("select itemName from Lskmzd where itemNo = :a");
                query.setString("a", itemNo);
                result = (String) query.uniqueResult();
            }
            //自定义字段
            if (itemType.equals("2")) {
                Query query = session1.createQuery("select itemName from Cbqtsz where itemNo = :a and itemType = :b");
                query.setString("a", itemNo);
                query.setString("b", "1");
                result = (String) query.uniqueResult();
            }
            tx1.commit();
            close(session1);
            return result;
        } catch (Exception e) {
            close(session1);
            e.printStackTrace();
            return null;
        }
    }

    //获取Cbtbsz里的graphData1或graphData2
    public String getGraphData(String dataNo, String graphNo) {
        Session session1 = null;
        Transaction tx1 = null;
        String result = null;
        try {
            session1 = getSession();
            tx1 = session1.beginTransaction();

            if (dataNo.equals("1")) {
                Query query_ItemNo1 = session1.createQuery("select graphData1 from Cbtbsz where graphNo = :a");
                query_ItemNo1.setString("a", graphNo);
                result = (String) query_ItemNo1.uniqueResult();
            } else {
                Query query_ItemNo1 = session1.createQuery("select graphData2 from Cbtbsz where graphNo = :a");
                query_ItemNo1.setString("a", graphNo);
                result = (String) query_ItemNo1.uniqueResult();
            }
            tx1.commit();
            close(session1);
            return result;
        } catch (Exception e) {
            tx1.rollback();
            close(session1);
            e.printStackTrace();
            return null;
        }
    }

    //获取对应的金额（普通科目）
    public String getItemNoMoney(String itemNo, String year, String sqlField_Summation) {
        String DefinedItemNoMoney = null;   //返回值(包含今年金额和去年金额)
        Double moneyNow = 0.0;
        Double moneyLast = 0.0;
        Session session = null;
        Transaction tx = null;

        try {
            session = getSession();
            tx = session.beginTransaction();

            //判断如果年份是今年
            if (year.equals("now")) {
                Query query = session.createQuery("select " + sqlField_Summation + " from Lskmzd where itemNo = :a");
                query.setString("a", itemNo);
                if (query.uniqueResult() != null) {
                    moneyNow = (Double) query.uniqueResult();
                }
                DefinedItemNoMoney = moneyNow.toString();
            } else {//判断如果年份是去年
                Calendar cal = Calendar.getInstance();
                int yearNow = cal.get(Calendar.YEAR) - 1;
                String FormName = "Lskmzd" + yearNow;
                String sql = "select " + sqlField_Summation + " from " + FormName + " where itemNo = ?";
                Query query = session.createSQLQuery(sql);
                query.setString(0, itemNo);
                if (query.uniqueResult() != null) {
                    moneyLast = (double) query.uniqueResult();
                }
                DefinedItemNoMoney = moneyLast.toString();
            }
            tx.commit();
            close(session);
        } catch (Exception e) {
            close(session);
            e.printStackTrace();
        }

        return DefinedItemNoMoney;
    }

    //获取具体某月的产量（普通科目）
    public Double getItemNoProduction(String itemNo, String year, String sqlFiled_Production) {
        Session session = null;
        Transaction tx = null;
        List<String> itemList_defined = new ArrayList<>();
        Double definedItemProductionSum = 0.0;

        session = getSession();
        tx = session.beginTransaction();
        try {
            //判断如果年份是今年
            if (year.equals("now")) {
                Query query_production1 = session.createQuery("select sum(" + sqlFiled_Production + ") from Lshssl where itemNo = :a");
                query_production1.setString("a", itemNo);
                if (query_production1.uniqueResult() != null) {
                    definedItemProductionSum = (double) query_production1.uniqueResult();
                }

            } else {//判断如果年份是去年
                Calendar cal = Calendar.getInstance();
                int yearLast = cal.get(Calendar.YEAR) - 1;
                String FormName = "Lshssl" + yearLast;

                String sql = "select sum(" + sqlFiled_Production + ") from " + FormName + " where itemNo = ?";
                Query query = session.createSQLQuery(sql);
                query.setString(0, itemNo);
                if (query.uniqueResult() != null) {
                    definedItemProductionSum = (double) query.uniqueResult();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        tx.commit();
        close(session);
        return definedItemProductionSum;
    }

    //根据自定义科目获取相应的科目编号
    public List<String> getItemNoByDefined(String DefinedItemNo) {
        Session session1 = null;
        Transaction tx1 = null;
        session1 = getSession();
        tx1 = session1.beginTransaction();
        List<String> itemNoList = new ArrayList<>();

        Query query = session1.createQuery("select itemData1 from Cbqtsz where itemNo = :a");
        query.setString("a", DefinedItemNo);
        String itemNo = (String) query.uniqueResult();
        if (itemNo != null) {
            String[] split_itemNo = itemNo.split("\\;");
            //将数组的每个数据加进list
            for (String item1 : split_itemNo) {
                itemNoList.add(item1);
            }
        }
        tx1.commit();
        close(session1);
        return itemNoList;
    }

    //获取具体某月的商品量（自定义字段）
    public Double getDefinedItemNoProduction(String itemDefinedNo, String year, String sqlFiled_Production) {
        Session session = null;
        Transaction tx = null;
        List<String> itemList_defined = new ArrayList<>();
        Double definedItemProductionSum = 0.0;

        itemList_defined = getItemNoByDefined(itemDefinedNo);//根据自定义字段编号获取对应的科目编号列

        session = getSession();
        tx = session.beginTransaction();
        for (String itemNo : itemList_defined) {
            try {
                //判断如果年份是今年
                if (year.equals("now")) {
                    Query query_production1 = session.createQuery("select sum(" + sqlFiled_Production + ") from Lshssl where itemNo = :a");
                    query_production1.setString("a", itemNo);
                    if (query_production1.uniqueResult() != null) {
                        definedItemProductionSum += (double) query_production1.uniqueResult();
                    }

                } else {//判断如果年份是去年
                    Calendar cal = Calendar.getInstance();
                    int yearLast = cal.get(Calendar.YEAR) - 1;
                    String FormName = "Lshssl" + yearLast;

                    String sql = "select sum(" + sqlFiled_Production + ") from " + FormName + " where itemNo = ?";
                    Query query = session.createSQLQuery(sql);
                    query.setString(0, itemNo);
                    if (query.uniqueResult() != null) {
                        definedItemProductionSum += (double) query.uniqueResult();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        tx.commit();
        close(session);
        return definedItemProductionSum;
    }

    //获取对应的金额（自定义字段）
    public String getDefinedItemNoMoney(String itemDefinedNo, String year, String sqlField_Summation) {
        String DefinedItemNoMoney = null;   //返回值(包含今年金额和去年金额)
        Double moneyNow = 0.0;
        Double moneyLast = 0.0;
        Session session = null;
        Transaction tx = null;

        try {
            List<String> itemList = getItemNoByDefined(itemDefinedNo);

            session = getSession();
            tx = session.beginTransaction();

            //判断如果年份是今年
            if (year.equals("now")) {
                for (String itemNo : itemList) {
                    Query query = session.createQuery("select " + sqlField_Summation + " from Lskmzd where itemNo = :a");
                    query.setString("a", itemNo);
                    if (query.uniqueResult() != null) {
                        moneyNow += (Double) query.uniqueResult();
                    }
                }
                DefinedItemNoMoney = moneyNow.toString();
                //判断如果年份是去年
            } else {
                Calendar cal = Calendar.getInstance();
                int yearNow = cal.get(Calendar.YEAR) - 1;
                String FormName = "Lskmzd" + yearNow;
                for (String itemNo : itemList) {
                    String sql = "select " + sqlField_Summation + " from " + FormName + " where itemNo = ?";
                    Query query = session.createSQLQuery(sql);
                    query.setString(0, itemNo);
                    if (query.uniqueResult() != null) {
                        moneyLast += (double) query.uniqueResult();
                    }
                }
                DefinedItemNoMoney = moneyLast.toString();
            }
            tx.commit();
            close(session);
        } catch (Exception e) {
            //tx.rollback();
            close(session);
            e.printStackTrace();
        }

        return DefinedItemNoMoney;
    }

    //根据科目编号判断该科目是普通科目还是自定义字段
    public String judgeTypeByItemNo(String itemNo) {
        String itemType = "0";//返回值，编号类型
        List<String> itemNoList = new ArrayList<>();//普通科目列表
        List<String> itemNoList_defined = new ArrayList<>();//自定义字段列表

        //普通科目
        String itemNolist1 = getGraphData("1", "000014");
        if (itemNolist1 != null) {
            String[] split_itemNo = itemNolist1.split("\\;");
            for (String item1 : split_itemNo) {
                itemNoList.add(item1);
            }
        }
        //自定义字段
        String itemNolist2 = getGraphData("2", "000014");
        if (itemNolist2 != null) {
            String[] split_itemNo1 = itemNolist2.split("\\;");
            for (String item1 : split_itemNo1) {
                itemNoList_defined.add(item1);
            }
        }

        //标记一下编号类型
        if (itemNoList.contains(itemNo)) {
            itemType = "1";
        }
        if (itemNoList_defined.contains(itemNo)) {
            itemType = "2";
        }
        return itemType;
    }

    //判断两个数是否可以相除
    static double calculateData(double a, double b) {
        if (b != 0) {
            return a / b;
        } else {
            return 0;
        }
    }
}
