package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.model.ModelGroup;
import com.iflytek.vie.utils.DBSelectUtil;
import com.iflytek.vie.utils.ListUtil;
import com.iflytek.vie.utils.StringUtils;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.internal.SessionFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate4.HibernateCallback;
import org.springframework.orm.hibernate4.HibernateTemplate;

public class ModelGroupDBPlatform {
   private HibernateTemplate hiberTemp;
   private JdbcTemplate jdbcTemplate;
   private final Logger logger = LoggerFactory.getLogger(ModelGroupDBPlatform.class);

   public ModelGroup queryModelGroupByID(long modelGroupId) {
      Session session = null;

      ModelGroup var7;
      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String hql = "select mg from ModelGroup mg where mg.groupId= " + modelGroupId + " and mg.isDisplay=1 and mg.deleted=0";
         Query query = session.createQuery(hql);
         List<ModelGroup> mgList = query.list();
         if (ListUtil.isNullOrEmpty(mgList)) {
            return null;
         }

         var7 = mgList.get(0);
      } catch (Exception var11) {
         this.logger.error("查询模型组异常", var11);
         return null;
      } finally {
         session.close();
      }

      return var7;
   }

   public boolean queryModelOnlineByGroupId(long modelGroupId, String dataSource) {
      Session session = null;

      boolean var7;
      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String sql = "";
         if (DBSelectUtil.isSqlServer()) {
            sql = "WITH groupTemp (GROUP_ID, PARENT_ID) AS ( SELECT GROUP_ID, PARENT_ID FROM  MODEL_GROUP WHERE DATASOURCE = '"
               + dataSource
               + "' AND  GROUP_ID ="
               + modelGroupId
               + " UNION ALL "
               + " SELECT  child.GROUP_ID, child.PARENT_ID FROM MODEL_GROUP AS child,groupTemp AS parent "
               + " WHERE parent.GROUP_ID = child.PARENT_ID "
               + ") SELECT COUNT (mi.model_id) FROM MODEL_INFO mi "
               + " JOIN (SELECT mt.model_id FROM MODEL_TASK_T mt "
               + " LEFT JOIN TASK_INFO_T ti ON mt.task_id = ti.task_id"
               + " WHERE ti.run_schema = 0 AND ti.task_status = 1 "
               + ") task ON mi.model_id = task.model_id"
               + " WHERE "
               + " mi.model_status = 0 AND mi.datasource = '"
               + dataSource
               + "' AND mi.group_id IN ( SELECT groupTemp.GROUP_ID FROM groupTemp )";
         } else if (DBSelectUtil.isMysql()) {
            String childSql = "select tg.group_id tmd from model_group tg where FIND_IN_SET(group_id, getChildList(" + modelGroupId + "))";
            sql = " SELECT count(mi.model_id) FROM MODEL_INFO mi JOIN (SELECT mt.model_id, mt.task_id, ti.task_status FROM MODEL_TASK_T mt LEFT JOIN TASK_INFO_T ti ON mt.task_id = ti.task_id WHERE ti.run_schema = 0 and ti.task_status = 1) task ON mi.model_id = task.model_id where mi.model_status = 0 and mi.datasource = '"
               + dataSource
               + "' and mi.group_id in (select groupTemp.tmd from ("
               + childSql
               + " groupTemp))";
         } else {
            sql = "with groupTemp as  (select tg.group_id tmd from MODEL_GROUP tg  start with tg.group_id ="
               + modelGroupId
               + " connect by tg.parent_id = prior tg.group_id)"
               + " SELECT count(mi.model_id)"
               + " FROM MODEL_INFO mi"
               + " JOIN (SELECT mt.model_id, mt.task_id, ti.task_status"
               + " FROM MODEL_TASK_T mt"
               + " LEFT JOIN TASK_INFO_T ti"
               + " ON mt.task_id = ti.task_id"
               + " WHERE ti.run_schema = 0"
               + " and ti.task_status = 1) task"
               + " ON mi.model_id = task.model_id"
               + " where mi.model_status = 0"
               + " and mi.datasource = '"
               + dataSource
               + "' and mi.group_id in (select groupTemp.tmd from groupTemp)";
         }

         System.out.println("search group sql:" + sql);
         Object obj = session.createSQLQuery(sql).uniqueResult();
         if (Integer.parseInt(String.valueOf(obj)) == 0) {
            return false;
         }

         var7 = true;
      } catch (Exception var11) {
         this.logger.error("method queryModelOnlineByGroupId exception");
         return false;
      } finally {
         if (session != null) {
            session.close();
         }
      }

      return var7;
   }

   public List<Long> queryAllModelGroupIdsByPermission(List<Long> modelGroupIds, String dataSource) throws Exception {
      List<ModelGroup> modelGroups = this.queryGroupsByPermission(modelGroupIds, dataSource);
      List<Long> groupIds = new ArrayList<>();
      if (modelGroupIds != null && modelGroupIds.size() > 0) {
         for (ModelGroup modelGroup : modelGroups) {
            groupIds.add(modelGroup.getGroupId());
         }
      }

      return groupIds;
   }

   public List<ModelGroup> queryAllModelGroupsByPermission(List<Long> modelGroupIds, String dataSource) {
      List<ModelGroup> modelGroups = new ArrayList<>();
      Session session = null;

      Object var24;
      try {
         StringBuffer sb = new StringBuffer();

         for (int i = 0; i < modelGroupIds.size(); i++) {
            sb.append(modelGroupIds.get(i) + ",");
         }

         String modelGroupId = "";
         if (sb.length() == 0) {
            modelGroupId = "0";
         } else {
            modelGroupId = sb.substring(0, sb.length() - 1);
         }

         String sql = "";
         session = this.hiberTemp.getSessionFactory().openSession();
         if (DBSelectUtil.isSqlServer()) {
            sql = "with modelgroup(group_id,group_name,parent_id) as(select group_id,group_name,parent_id from model_group where datasource = '"
               + dataSource
               + "' and  group_id IN ("
               + modelGroupId
               + ") union all select k.group_id,k.group_name,k.parent_id from model_group k "
               + "inner join modelgroup on k.parent_id = modelgroup.group_id) "
               + "select group_id,group_name,parent_id from modelgroup order by group_id";
         } else if (!DBSelectUtil.isMysql()) {
            sql = "SELECT group_id,group_name,parent_id from model_group where datasource = '"
               + dataSource
               + "'  start with group_id IN ("
               + modelGroupId
               + ") "
               + "connect by prior group_id = parent_id order by group_id";
         } else {
            List<Object> h = new ArrayList<>();

            for (int i = 0; i < modelGroupIds.size(); i++) {
               String rec = "SELECT group_id,group_name,parent_id from model_group where datasource = '"
                  + dataSource
                  + "'and FIND_IN_SET(group_id, getChildList("
                  + modelGroupIds.get(i)
                  + ")) order by group_id";
               Query query = session.createSQLQuery(rec);
               List<Object[]> list = query.list();
               if (list != null && list.size() > 0) {
                  for (Object[] objects : list) {
                     if (!h.contains(objects[0])) {
                        modelGroups.add(this.objectsToModelGroup(objects));
                        h.add(objects[0]);
                     }
                  }
               }
            }
         }

         if (DBSelectUtil.isMysql()) {
            return modelGroups;
         }

         Query query = session.createSQLQuery(sql);
         List<Object[]> list = query.list();
         if (list != null) {
            for (Object[] objectsx : list) {
               modelGroups.add(this.objectsToModelGroup(objectsx));
            }

            return modelGroups;
         }

         var24 = modelGroups;
      } catch (Exception var18) {
         this.logger.error("method queryAllModelGroupsByPermission exception");
         return modelGroups;
      } finally {
         if (session != null) {
            session.close();
         }
      }

      return (List<ModelGroup>)var24;
   }

   public List<ModelGroup> queryGroupsByPermission(List<Long> modelGroupIds, String dataSource) throws Exception {
      if (modelGroupIds != null && modelGroupIds.size() != 0) {
         StringBuffer sb = this.splicingModelGroupIds(this.removeDuplicate(modelGroupIds));
         List<ModelGroup> modelGroups = new ArrayList<>();
         String sql = "";
         if (DBSelectUtil.isSqlServer()) {
            sql = "with modelgroup(group_id,group_name,parent_id) as(select group_id,group_name,parent_id from model_group where datasource = '"
               + dataSource
               + "' and  group_id IN ("
               + sb.toString()
               + ") union all select k.group_id,k.group_name,k.parent_id from model_group k "
               + "inner join modelgroup on k.parent_id = modelgroup.group_id) "
               + "select group_id,group_name,parent_id from modelgroup order by group_id";
         } else if (DBSelectUtil.isMysql()) {
            List<Object> h = new ArrayList<>();

            for (int i = 0; i < modelGroupIds.size(); i++) {
               String rec = "SELECT group_id,group_name,parent_id from model_group where datasource = '"
                  + dataSource
                  + "' and FIND_IN_SET(group_id, getChildList("
                  + modelGroupIds.get(i)
                  + ")) order by group_id";
               List<Map<String, Object>> one = this.jdbcTemplate.queryForList(rec);
               if (one != null && one.size() > 0) {
                  for (Map<String, Object> map : (List<Map<String, Object>>)one) {
                     if (!h.contains(map.get("group_id"))) {
                        modelGroups.add(this.mapToModelGroup(map));
                        h.add(map.get("group_id"));
                     }
                  }
               }
            }
         } else {
            sql = "SELECT group_id,group_name,parent_id from model_group where datasource = '"
               + dataSource
               + "' start with group_id IN ("
               + sb.toString()
               + ") "
               + "connect by prior group_id = parent_id order by group_id";
         }

         if (!DBSelectUtil.isMysql()) {
            List<Map<String, Object>> list = this.jdbcTemplate.queryForList(sql);
            if (list == null) {
               return modelGroups;
            }

            for (Map<String, Object> mapx : (List<Map<String, Object>>)list) {
               modelGroups.add(this.mapToModelGroup(mapx));
            }
         }

         return modelGroups;
      } else {
         return null;
      }
   }

   private List<Long> removeDuplicate(List<Long> modelGroupIds) {
      LinkedHashSet<Long> set = new LinkedHashSet<>(modelGroupIds.size());
      set.addAll(modelGroupIds);
      modelGroupIds.clear();
      modelGroupIds.addAll(set);
      return modelGroupIds;
   }

   private StringBuffer splicingModelGroupIds(List<Long> modelGroupIds) {
      StringBuffer sb = new StringBuffer();
      int inNum = 1;

      for (int i = 0; i < modelGroupIds.size(); i++) {
         if (!StringUtils.isNullOrEmpry(modelGroupIds.get(i) + "")) {
            if (i == modelGroupIds.size() - 1) {
               sb.append(modelGroupIds.get(i) + "");
            } else if (inNum == 1000 && i > 0) {
               sb.append(modelGroupIds.get(i) + " ) or group_id in ( ");
               inNum = 1;
            } else {
               sb.append(modelGroupIds.get(i) + ",");
               inNum++;
            }
         }
      }

      return sb;
   }

   public ModelGroup objectsToModelGroup(Object[] objects) {
      ModelGroup modelGroup = new ModelGroup();
      modelGroup.setGroupId(Long.parseLong(String.valueOf(objects[0])));
      modelGroup.setGroupName(String.valueOf(objects[1]));
      modelGroup.setParentId(Long.parseLong(String.valueOf(objects[2])));
      return modelGroup;
   }

   public ModelGroup mapToModelGroup(Map<String, Object> map) {
      ModelGroup modelGroup = new ModelGroup();
      modelGroup.setGroupId(Long.parseLong(String.valueOf(map.get("group_id"))));
      modelGroup.setGroupName(String.valueOf(map.get("group_name")));
      modelGroup.setParentId(Long.parseLong(String.valueOf(map.get("parent_id"))));
      return modelGroup;
   }

   public List getModelGroupIdById(long modelGroupId, String dataSource) {
      Session session = null;

      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String sql = "";
         if (DBSelectUtil.isSqlServer()) {
            sql = "with modelgroup(group_id,parent_id,group_name) as (select group_id,parent_id,group_name from model_group where datasource = '"
               + dataSource
               + "' and  group_id = "
               + modelGroupId
               + " union all "
               + "select k.group_id,k.parent_id,k.group_name from model_group k "
               + "inner join modelgroup on k.parent_id = modelgroup.group_id)"
               + "select t.group_id  "
               + "from modelgroup t ";
         } else if (DBSelectUtil.isMysql()) {
            sql = " SELECT group_id from model_group where datasource = '" + dataSource + "' and FIND_IN_SET(group_id, getChildList(" + modelGroupId + "))";
         } else {
            sql = "select t.group_id  from model_group t where t.datasource = '"
               + dataSource
               + "' "
               + " start with t.group_id="
               + modelGroupId
               + " connect by prior t.group_id=t.parent_id";
         }

         Query query = session.createSQLQuery(sql);
         return query.list();
      } catch (Exception var12) {
         this.logger.error("根据模型组id查询子模型组ID异常", var12);
      } finally {
         session.close();
      }

      return null;
   }

   public List<Long> getModelGroupIdById(List<Long> modelGroupIds, String dataSource) {
      if (modelGroupIds != null && modelGroupIds.size() != 0) {
         List<Long> resultList = new ArrayList<>();
         Session session = null;

         try {
            StringBuffer sb = this.splicingModelGroupIds(this.removeDuplicate(modelGroupIds));
            session = this.hiberTemp.getSessionFactory().openSession();
            String sql = "";
            if (DBSelectUtil.isSqlServer()) {
               sql = "with modelgroup(group_id,parent_id,group_name) as (select group_id,parent_id,group_name from model_group where datasource = '"
                  + dataSource
                  + "' and group_id in ("
                  + sb.toString()
                  + ") union all "
                  + "select k.group_id,k.parent_id,k.group_name from model_group k "
                  + "inner join modelgroup on k.parent_id = modelgroup.group_id)"
                  + "select t.group_id,t.group_name  "
                  + "from modelgroup t ";
            } else if (!DBSelectUtil.isMysql()) {
               sql = "select group_id,group_name  from model_group where datasource = '"
                  + dataSource
                  + "' "
                  + " start with group_id in ("
                  + sb.toString()
                  + ") connect by prior group_id=parent_id";
            } else {
               List<Object> h = new ArrayList<>();

               for (int i = 0; i < modelGroupIds.size(); i++) {
                  String aa = "SELECT group_id,group_name from model_group where datasource = '"
                     + dataSource
                     + "' and FIND_IN_SET(group_id, getChildList("
                     + modelGroupIds.get(i)
                     + "))";
                  Query query = session.createSQLQuery(aa);
                  List<Object[]> one = query.list();
                  if (one != null && one.size() > 0) {
                     for (Object[] aaa : one) {
                        if (!h.contains(aaa[0])) {
                           resultList.add(Long.parseLong(aaa[0].toString()));
                           h.add(aaa[0]);
                        }
                     }
                  }
               }
            }

            if (!DBSelectUtil.isMysql()) {
               Query query = session.createSQLQuery(sql);
               List<Object[]> modelGrouIdList = query.list();
               if (modelGrouIdList == null) {
                  return null;
               }

               for (Object[] aa : modelGrouIdList) {
                  resultList.add(Long.parseLong(aa[0].toString()));
               }
            }

            return resultList;
         } catch (Exception var17) {
            this.logger.error("根据模型组ID查询模型组异常", var17);
            return null;
         } finally {
            session.close();
         }
      } else {
         return null;
      }
   }

   public boolean isSameModelGroup(String modelGroupName, long modelGroupId, List<Long> groupIds, String dataSource) {
      List<ModelGroup> modelGroups = new ArrayList<>();

      try {
         List<Object[]> list = this.searchModelGroups(groupIds, modelGroupName, dataSource);
         if (list == null) {
            return false;
         } else {
            for (Object[] objects : list) {
               modelGroups.add(this.objectsToModelGroup(objects));
            }

            if (modelGroups != null && modelGroups.size() >= 1) {
               for (ModelGroup mg : modelGroups) {
                  if (mg.getParentId() == modelGroupId && mg.getGroupName() != null && mg.getGroupName().equals(modelGroupName)) {
                     return true;
                  }
               }

               return false;
            } else {
               return false;
            }
         }
      } catch (Exception var10) {
         this.logger.error("模型组是否重名异常", var10);
         return true;
      }
   }

   public List<Object[]> searchModelGroups(List<Long> groupIds, String modelGroupName, String dataSource) {
      if (groupIds != null && groupIds.size() != 0) {
         Session session = null;
         StringBuffer sb = this.splicingModelGroupIds(this.removeDuplicate(groupIds));
         session = this.hiberTemp.getSessionFactory().openSession();

         try {
            String escapeStr = "";
            String sql = "";
            if (!StringUtils.isNullOrEmpry(modelGroupName)) {
               modelGroupName = modelGroupName.replace("_", "\\_");
               modelGroupName = modelGroupName.replace("%", "\\%");
               if ("\\".equals(modelGroupName)) {
                  escapeStr = " ESCAPE '\\'";
               }

               if (DBSelectUtil.isSqlServer()) {
                  sql = "with modelgroup(group_id,group_name,parent_id,tree_code_num) as(select group_id,group_name,parent_id,tree_code_num from model_group where datasource = '"
                     + dataSource
                     + "' and  group_name like '%"
                     + modelGroupName
                     + "%'"
                     + escapeStr
                     + " and is_topic = 0 and group_id in ("
                     + sb.toString()
                     + ")"
                     + " union all "
                     + "select k.group_id,k.group_name,k.parent_id,k.tree_code_num from model_group k "
                     + "inner join modelgroup on k.parent_id = modelgroup.group_id) "
                     + "select group_id,group_name,parent_id,tree_code_num from modelgroup order by parent_id";
               } else {
                  if (DBSelectUtil.isMysql()) {
                     List<Object[]> modelGroupList = new ArrayList<>();
                     List<Object> groupId = new ArrayList<>();
                     String group_name = "'%" + modelGroupName + "%'" + escapeStr;

                     for (int i = 0; i < groupIds.size(); i++) {
                        String rec = "SELECT distinct(group_id) as group_id,group_name,parent_id,tree_code_num from model_group t where t.datasource = '"
                           + dataSource
                           + "' and FIND_IN_SET(group_id, getParentList("
                           + groupIds.get(i)
                           + ","
                           + group_name
                           + ")) order by t.parent_id";
                        Query query = session.createSQLQuery(rec);
                        List<Object[]> one = query.list();
                        if (one != null && one.size() > 0) {
                           for (int j = 0; j < one.size(); j++) {
                              if (!groupId.contains(one.get(j)[0])) {
                                 modelGroupList.add(one.get(j));
                                 groupId.add(one.get(j)[0]);
                              }
                           }
                        }
                     }

                     return modelGroupList;
                  }

                  sql = "select distinct(group_id) as group_id,group_name,parent_id,tree_code_num from model_group t where t.datasource = '"
                     + dataSource
                     + "' "
                     + " start with t.group_name like '%"
                     + modelGroupName
                     + "%'"
                     + escapeStr
                     + " and t.is_topic = 0 and t.group_id IN ("
                     + sb.toString()
                     + ")"
                     + " connect by prior t.parent_id = t.group_id order by t.parent_id ";
               }
            } else if (DBSelectUtil.isSqlServer()) {
               sql = "with modelgroup(group_id,group_name,parent_id,tree_code_num) as(select group_id,group_name,parent_id,tree_code_num from model_group where datasource = '"
                  + dataSource
                  + "' and group_id in ("
                  + sb.toString()
                  + ")"
                  + " union all "
                  + "select k.group_id,k.group_name,k.parent_id,k.tree_code_num from model_group k "
                  + "inner join modelgroup on k.parent_id = modelgroup.group_id) "
                  + "select group_id,group_name,parent_id,tree_code_num from modelgroup order by parent_id";
            } else {
               if (DBSelectUtil.isMysql()) {
                  List<Object[]> arrayList = new ArrayList<>();
                  List<Object> groupId = new ArrayList<>();

                  for (int ix = 0; ix < groupIds.size(); ix++) {
                     String rec = "SELECT distinct(group_id) as group_id,group_name,parent_id,tree_code_num from model_group t where t.datasource = '"
                        + dataSource
                        + "' and t.is_topic = 0 and FIND_IN_SET(group_id, getChildList("
                        + groupIds.get(ix)
                        + ")) order by t.parent_id";
                     Query query = session.createSQLQuery(rec);
                     List<Object[]> objects = query.list();
                     if (objects != null && objects.size() > 0) {
                        for (int jx = 0; jx < objects.size(); jx++) {
                           if (!groupId.contains(objects.get(jx)[0])) {
                              arrayList.add(objects.get(jx));
                              groupId.add(objects.get(jx)[0]);
                           }
                        }
                     }
                  }

                  return arrayList;
               }

               sql = "SELECT distinct(group_id) as group_id,group_name,parent_id,tree_code_num from model_group where datasource = '"
                  + dataSource
                  + "' start with is_topic = 0 and group_id IN ("
                  + sb.toString()
                  + ")  "
                  + " connect by prior group_id = parent_id order by parent_id";
            }

            Query query = session.createSQLQuery(sql);
            this.logger.info("searchModelGroups function execute sql is: " + sql);
            return query.list();
         } catch (Exception var19) {
            this.logger.error("查询所有模型组异常", var19);
            return null;
         } finally {
            if (session != null) {
               session.close();
            }
         }
      } else {
         return null;
      }
   }

   public Long addModelGroup(ModelGroup mg) {
      try {
         this.hiberTemp.save(mg);
         return mg.getGroupId();
      } catch (Exception var3) {
         this.logger.error("添加模型组异常", var3);
         return null;
      }
   }

   public boolean deleteModelGroup2(long modelGroupId, String dataSource) {
      Session session = null;

      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         if (DBSelectUtil.isSqlServer()) {
            String sql = "with modelgroup(group_id,parent_id) as (select group_id,parent_id from model_group where datasource = '"
               + dataSource
               + "' and group_id = "
               + modelGroupId
               + " union all "
               + "select k.group_id,k.parent_id from model_group k "
               + "inner join modelgroup on k.parent_id = modelgroup.group_id) "
               + "delete from task_info_t "
               + " where task_id in"
               + " (select mt.task_id"
               + " from model_task_t mt"
               + " where mt.model_id in"
               + " (select mi.model_id"
               + "  from model_info mi"
               + " where mi.datasource = '"
               + dataSource
               + "' and  mi.group_id in"
               + " (select group_id"
               + " from modelgroup)))";
            session.createSQLQuery(sql).executeUpdate();
            sql = "with modelgroup(group_id,parent_id) as (select group_id,parent_id from model_group where datasource = '"
               + dataSource
               + "' and group_id = "
               + modelGroupId
               + " union all "
               + "select k.group_id,k.parent_id from model_group k "
               + "inner join modelgroup on k.parent_id = modelgroup.group_id) "
               + "delete from model_task_t "
               + " where model_id in"
               + " (select mi.model_id"
               + "  from model_info mi"
               + " where mi.datasource = '"
               + dataSource
               + "' and  mi.group_id in"
               + " (select t.group_id"
               + " from modelgroup t))";
            session.createSQLQuery(sql).executeUpdate();
            sql = "with modelgroup(group_id,parent_id) as (select group_id,parent_id from model_group where datasource = '"
               + dataSource
               + "' and group_id = "
               + modelGroupId
               + " union all "
               + "select k.group_id,k.parent_id from model_group k "
               + "inner join modelgroup on k.parent_id = modelgroup.group_id) "
               + " delete from model_info where group_id in("
               + "select m.group_id from modelgroup m)";
            session.createSQLQuery(sql).executeUpdate();
            sql = "with modelgroup(group_id,parent_id) as (select group_id,parent_id from model_group where datasource = '"
               + dataSource
               + "' and group_id = "
               + modelGroupId
               + " union all "
               + "select k.group_id,k.parent_id from model_group k "
               + "inner join modelgroup on k.parent_id = modelgroup.group_id) "
               + " delete from model_group where group_id in("
               + "select m.group_id from modelgroup m)";
            session.createSQLQuery(sql).executeUpdate();
         } else if (DBSelectUtil.isMysql()) {
            String sql = "delete from task_info_t where task_id in (select mt.task_id from model_task_t mt where mt.model_id in (select mi.model_id  from model_info mi where mi.datasource = '"
               + dataSource
               + "' and mi.group_id in("
               + "select t.group_id"
               + " from model_group t where find_in_set(t.group_id,getChildList("
               + modelGroupId
               + ")))))";
            session.createSQLQuery(sql).executeUpdate();
            sql = "delete from model_task_t  where model_id in (select mi.model_id  from model_info mi where mi.datasource = '"
               + dataSource
               + "' and mi.group_id in("
               + "select t.group_id"
               + " from model_group t where find_in_set(t.group_id,getChildList("
               + modelGroupId
               + "))))";
            session.createSQLQuery(sql).executeUpdate();
            sql = " delete from model_info where datasource = '"
               + dataSource
               + "' and group_id in("
               + "select m.group_id from ("
               + "select t.group_id"
               + " from model_group t where find_in_set(t.group_id,getChildList("
               + modelGroupId
               + "))) m)";
            session.createSQLQuery(sql).executeUpdate();
            sql = " delete from model_group where datasource = '"
               + dataSource
               + "' and group_id in("
               + "select m.group_id from ("
               + "select t.group_id"
               + " from model_group t where find_in_set(t.group_id,getChildList("
               + modelGroupId
               + "))) m)";
            session.createSQLQuery(sql).executeUpdate();
         } else {
            String sql = "delete from task_info_t tt where tt.task_id in (select mt.task_id from model_task_t mt where mt.model_id in (select mi.model_id  from model_info mi where mi.datasource = '"
               + dataSource
               + "' and mi.group_id in"
               + " (select t.group_id"
               + " from model_group t"
               + " start with t.group_id = "
               + modelGroupId
               + " connect by prior t.group_id = t.parent_id)))";
            session.createSQLQuery(sql).executeUpdate();
            sql = "delete from model_task_t mt where mt.model_id in (select mi.model_id  from model_info mi where mi.datasource = '"
               + dataSource
               + "' and mi.group_id in"
               + " (select t.group_id"
               + " from model_group t"
               + " start with t.group_id = "
               + modelGroupId
               + " connect by prior t.group_id = t.parent_id))";
            session.createSQLQuery(sql).executeUpdate();
            sql = " delete from model_info mi where mi.datasource = '"
               + dataSource
               + "' and  mi.group_id in("
               + "select m.group_id from ("
               + "select t.group_id "
               + "from model_group t "
               + "start with t.group_id="
               + modelGroupId
               + "connect by prior t.group_id=t.parent_id) m)";
            session.createSQLQuery(sql).executeUpdate();
            sql = " delete from model_group mg where mg.datasource = '"
               + dataSource
               + "' and  mg.group_id in("
               + "select m.group_id from ("
               + "select t.group_id "
               + "from model_group t "
               + "start with t.group_id="
               + modelGroupId
               + "connect by prior t.group_id=t.parent_id) m)";
            session.createSQLQuery(sql).executeUpdate();
         }

         return true;
      } catch (Exception var9) {
         this.logger.error("删除模型组异常", var9);
      } finally {
         session.close();
      }

      return false;
   }

   public boolean saveOrUpdateModelGroup(ModelGroup mg) {
      try {
         this.hiberTemp.saveOrUpdate(mg);
         return true;
      } catch (Exception var3) {
         this.logger.error("更新模型组异常", var3);
         return false;
      }
   }

   public List<HashMap<String, Object>> findAllOnlineModelGroup(List<Long> groupId, String dataSource) {
      Connection conn = null;
      CallableStatement queryList = null;
      List<HashMap<String, Object>> dataList = new ArrayList<>();

      try {
         String sql = "select t.group_id,t.group_name,mi.model_id,mi.model_name,mi.text_rule,mi.model_uptonow from model_group t left join model_info mi on t.group_id=mi.group_id left join model_task_t t4 on mi.model_id = t4.model_id left join task_info_t t5 on t4.task_id = t5.task_id where t.datasource = '"
            + dataSource
            + "' and  t.deleted=0 and mi.model_status<>2 and t5.task_status = 1 "
            + (groupId.isEmpty() ? "" : "and t.group_id " + ListUtil.createIn(groupId, false))
            + "order by t.group_ID";
         conn = ((SessionFactoryImpl)this.hiberTemp.getSessionFactory()).getConnectionProvider().getConnection();
         queryList = conn.prepareCall(sql);
         queryList.execute();
         ResultSet rs = queryList.getResultSet();

         while (rs.next()) {
            HashMap<String, Object> data = new HashMap<>();
            data.put("group_id", rs.getString("group_id"));
            data.put("group_name", rs.getString("group_name"));
            data.put("model_id", rs.getString("model_id"));
            data.put("model_name", rs.getString("model_name"));
            data.put("text_rule", rs.getString("text_rule"));
            data.put("model_uptonow", rs.getString("model_uptonow"));
            dataList.add(data);
         }
      } catch (Exception var17) {
         this.logger.error("method findAllOnlineModelGroup exception");
      } finally {
         try {
            conn.close();
            queryList.close();
         } catch (SQLException var16) {
            this.logger.error("method findAllOnlineModelGroup exception");
         }
      }

      return dataList;
   }

   public List<String[]> queryAllCategoryModelList(final List<Long> groupIds, final String dataSource) {
      if (!DBSelectUtil.isMysql()) {
         String sql = "with modelTemp as (SELECT mi.model_id,mi.model_name   FROM MODEL_INFO mi  where mi.model_status = 0  and mi.datasource=:dataSource    and mi.group_id in (select tg.group_id tmd     from MODEL_GROUP tg    start with tg.group_id in :modeGroupId   connect by tg.parent_id = prior tg.group_id))   select mt.model_id,mt.model_name,mc.modelcategorytype from modelTemp mt   join MODEL_CATEGORY_RELEASSION mcr on mt.model_id=mcr.modelid   join MODEL_CATEGORY mc on mcr.modelcatagoryid=mc.id and mc.status=1";
         return (List<String[]>)this.hiberTemp
            .execute(
               new HibernateCallback<List<String[]>>() {
                  public List<String[]> doInHibernate(Session session) throws HibernateException {
                     List<Object[]> results = (List<Object[]>)(List<?>)session.createSQLQuery(
                           "with modelTemp as (SELECT mi.model_id,mi.model_name   FROM MODEL_INFO mi  where mi.model_status = 0  and mi.datasource=:dataSource    and mi.group_id in (select tg.group_id tmd     from MODEL_GROUP tg    start with tg.group_id in :modeGroupId   connect by tg.parent_id = prior tg.group_id))   select mt.model_id,mt.model_name,mc.modelcategorytype from modelTemp mt   join MODEL_CATEGORY_RELEASSION mcr on mt.model_id=mcr.modelid   join MODEL_CATEGORY mc on mcr.modelcatagoryid=mc.id and mc.status=1"
                        )
                        .setParameterList("modeGroupId", groupIds)
                        .setString("dataSource", dataSource)
                        .list();
                     if (results != null && results.size() != 0) {
                        Map<Long, String[]> maps = new HashMap<>();

                        for (int i = 0; i < results.size(); i++) {
                           Object[] objects = results.get(i);
                           String[] modelStr = null;
                           if (maps.containsKey(Long.parseLong(objects[0] + ""))) {
                              modelStr = maps.get(Long.parseLong(objects[0] + ""));
                              modelStr[2] = "3";
                           } else {
                              modelStr = new String[]{objects[0] + "", objects[1] + "", objects[2] + ""};
                           }

                           maps.put(Long.parseLong(objects[0] + ""), modelStr);
                        }

                        return new ArrayList<>(maps.values());
                     } else {
                        return null;
                     }
                  }
               }
            );
      } else {
         StringBuffer sb = new StringBuffer();

         for (int i = 0; i < groupIds.size(); i++) {
            String sql1 = "select group_id from MODEL_GROUP where find_in_set(group_id,getChildList(" + groupIds.get(i) + ")) ";
            List<Map<String, Object>> list = this.jdbcTemplate.queryForList(sql1);
            if (list != null && list.size() > 0) {
               for (Map map : list) {
                  sb.append(map.get("group_id") + ",");
               }
            }
         }

         String groups = sb.substring(0, sb.length() - 1);
         final String sql = "  select mt.model_id,mt.model_name,mc.modelcategorytype from (SELECT mi.model_id,mi.model_name   FROM MODEL_INFO mi  where mi.model_status = 0  and mi.datasource=:dataSource    and mi.group_id in ("
            + groups
            + ")) mt"
            + "  join MODEL_CATEGORY_RELEASSION mcr on mt.model_id=mcr.modelid "
            + "  join MODEL_CATEGORY mc on mcr.modelcatagoryid=mc.id and mc.status=1";
         return (List<String[]>)this.hiberTemp.execute(new HibernateCallback<List<String[]>>() {
            public List<String[]> doInHibernate(Session session) throws HibernateException {
               List<Object[]> results = (List<Object[]>)(List<?>)session.createSQLQuery(sql).setString("dataSource", dataSource).list();
               if (results != null && results.size() != 0) {
                  Map<Long, String[]> maps = new HashMap<>();

                  for (int i = 0; i < results.size(); i++) {
                     Object[] objects = results.get(i);
                     String[] modelStr = null;
                     if (maps.containsKey(Long.parseLong(objects[0] + ""))) {
                        modelStr = maps.get(Long.parseLong(objects[0] + ""));
                        modelStr[2] = "3";
                     } else {
                        modelStr = new String[]{objects[0] + "", objects[1] + "", objects[2] + ""};
                     }

                     maps.put(Long.parseLong(objects[0] + ""), modelStr);
                  }

                  return new ArrayList<>(maps.values());
               } else {
                  return null;
               }
            }
         });
      }
   }

   public HibernateTemplate getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplate hiberTemp) {
      this.hiberTemp = hiberTemp;
   }

   public JdbcTemplate getJdbcTemplate() {
      return this.jdbcTemplate;
   }

   public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
      this.jdbcTemplate = jdbcTemplate;
   }
}
