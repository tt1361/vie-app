package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.model.ModelGroup;
import com.iflytek.vie.utils.DBSelectUtil;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelGroupDB {
   private HibernateTemplateUtils hiberTemp;
   private static final Logger logger = LoggerFactory.getLogger(ModelGroupDB.class);

   public ModelGroup queryModelGroupByID(long modelGroupId) {
      try {
         StringBuffer hql = new StringBuffer("select mg from ModelGroup mg where mg.groupId = :modelGroupId and mg.isDisplay=1 and mg.deleted=0");
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         paramNames.add("modelGroupId");
         values.add(modelGroupId);
         return (ModelGroup)this.hiberTemp.uniqueResult(hql.toString(), paramNames, values);
      } catch (Exception var6) {
         logger.error("根据模型组ID查询模型组错误", var6);
         return null;
      }
   }

   public List<Long> queryAllModelGroupIdsByPermission(List<Long> modelGroupIds, String dataSource) {
      List<ModelGroup> modelGroups = this.queryGroupsByPermission(modelGroupIds, dataSource);
      List<Long> groupIds = new ArrayList<>();
      if (modelGroupIds != null && modelGroupIds.size() > 0) {
         for (ModelGroup modelGroup : modelGroups) {
            groupIds.add(modelGroup.getGroupId());
         }
      }

      return groupIds;
   }

   public List<ModelGroup> queryGroupsByPermission(List<Long> modelGroupIds, String dataSource) {
      List<ModelGroup> modelGroups = new ArrayList<>();
      Session session = null;

      Object var21;
      try {
         StringBuffer sb = this.splicingModelGroupIds(
            modelGroupIds != null && modelGroupIds.size() > 0 ? this.removeDuplicate(modelGroupIds) : modelGroupIds, "group_id"
         );
         String sql = "";
         session = this.hiberTemp.getSessionFactory().openSession();
         if (DBSelectUtil.isSqlServer()) {
            sql = "with modelgroup(group_id,group_name,parent_id) as(select group_id,group_name,parent_id from model_group where group_id IN ("
               + sb.toString()
               + ") and datasource = '"
               + dataSource
               + "' union all select k.group_id,k.group_name,k.parent_id from model_group k "
               + "inner join modelgroup on k.parent_id = modelgroup.group_id) "
               + "select group_id,group_name,parent_id from modelgroup order by group_id";
         } else if (!DBSelectUtil.isMysql()) {
            sql = "SELECT group_id,group_name,parent_id from model_group where datasource = '"
               + dataSource
               + "' start with group_id IN ("
               + sb.toString()
               + ") "
               + "connect by prior group_id = parent_id order by group_id";
         } else {
            List<Object> h = new ArrayList<>();

            for (int i = 0; i < modelGroupIds.size(); i++) {
               String rec = "SELECT group_id,group_name,parent_id from model_group where datasource = '"
                  + dataSource
                  + "' and find_in_set(group_id,getChildList("
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

         var21 = modelGroups;
      } catch (Exception var17) {
         logger.error("查询权限系统模型组错误", var17);
         return modelGroups;
      } finally {
         if (session != null) {
            session.close();
         }
      }

      return (List<ModelGroup>)var21;
   }

   private List<Long> removeDuplicate(List<Long> modelGroupIds) {
      LinkedHashSet<Long> set = new LinkedHashSet<>(modelGroupIds.size());
      set.addAll(modelGroupIds);
      modelGroupIds.clear();
      modelGroupIds.addAll(set);
      return modelGroupIds;
   }

   private StringBuffer splicingModelGroupIds(List<Long> modelGroupIds, String row) {
      StringBuffer sb = new StringBuffer();
      int num = 1;

      for (int i = 0; i < modelGroupIds.size(); i++) {
         if (i == modelGroupIds.size() - 1) {
            sb.append(modelGroupIds.get(i) + "");
         } else if (num == 1000 && i > 0) {
            sb.append(modelGroupIds.get(i) + " ) or " + row + " in ( ");
            num = 1;
         } else {
            sb.append(modelGroupIds.get(i) + ",");
            num++;
         }
      }

      return sb;
   }

   public ModelGroup objectsToModelGroup(Object[] objects) {
      ModelGroup modelGroup = new ModelGroup();
      modelGroup.setGroupId(Long.parseLong(String.valueOf(objects[0])));
      modelGroup.setGroupName(String.valueOf(objects[1]));
      return modelGroup;
   }

   public List<Long> getModelGroupIdById(List<Long> modelGroupIds, String dataSource) {
      if (modelGroupIds != null && modelGroupIds.size() != 0) {
         Session session = null;

         try {
            StringBuffer sb = this.splicingModelGroupIds(this.removeDuplicate(modelGroupIds), "group_id");
            session = this.hiberTemp.getSessionFactory().openSession();
            String sql = "";
            if (DBSelectUtil.isSqlServer()) {
               sql = "with modelgroup(group_id,parent_id,group_name) as (select group_id,parent_id,group_name from model_group where group_id in ("
                  + sb.toString()
                  + ") and datasource = '"
                  + dataSource
                  + "' union all "
                  + "select k.group_id,k.parent_id,k.group_name from model_group k "
                  + "inner join modelgroup on k.parent_id = modelgroup.group_id where k.datasource = '"
                  + dataSource
                  + "') "
                  + "select t.group_id,t.group_name  "
                  + "from modelgroup t ";
            } else {
               if (DBSelectUtil.isMysql()) {
                  List<Long> resultList = new ArrayList<>();
                  List<Long> groupIds = new ArrayList<>();

                  for (int i = 0; i < modelGroupIds.size(); i++) {
                     String rec = "SELECT group_id,group_name,parent_id from model_group where datasource = '"
                        + dataSource
                        + "' and find_in_set(group_id,getChildList("
                        + modelGroupIds.get(i)
                        + ")) order by group_id";
                     Query query = session.createSQLQuery(rec);
                     List<Object[]> modelGrouIdList = query.list();
                     if (modelGrouIdList != null && !modelGrouIdList.isEmpty()) {
                        for (Object[] aa : modelGrouIdList) {
                           if (!groupIds.contains(Long.parseLong(aa[0].toString()))) {
                              resultList.add(Long.parseLong(aa[0].toString()));
                              groupIds.add(Long.parseLong(aa[0].toString()));
                           }
                        }
                     }
                  }

                  return resultList;
               }

               sql = "select group_id,group_name  from model_group where datasource = '"
                  + dataSource
                  + "' "
                  + " start with group_id in ("
                  + sb.toString()
                  + ") connect by prior group_id=parent_id";
            }

            if (DBSelectUtil.isMysql()) {
               return null;
            } else {
               Query query = session.createSQLQuery(sql);
               List<Object[]> modelGrouIdList = query.list();
               List<Long> resultList = new ArrayList<>();
               if (modelGrouIdList == null) {
                  return null;
               } else {
                  for (Object[] aax : modelGrouIdList) {
                     resultList.add(Long.parseLong(aax[0].toString()));
                  }

                  return resultList;
               }
            }
         } catch (Exception var17) {
            logger.error("根据模型组ID查询模型组异常", var17);
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
         logger.error("判断模型组是否重名错误", var10);
         return true;
      }
   }

   public List<Object[]> searchModelGroups(List<Long> groupIds, String modelGroupName, String dataSource) {
      Session session = null;
      StringBuffer sb = new StringBuffer();

      for (int i = 0; i < groupIds.size(); i++) {
         sb.append(groupIds.get(i) + ",");
      }

      String modelGroups = sb.substring(0, sb.length() - 1);

      try {
         String escapeStr = "";
         String sql = "";
         session = this.hiberTemp.getSessionFactory().openSession();
         if (!StringUtils.isNullOrEmpry(modelGroupName)) {
            modelGroupName = modelGroupName.replace("_", "\\_");
            if ("\\".equals(modelGroupName)) {
               escapeStr = " ESCAPE '\\'";
            }

            if (DBSelectUtil.isSqlServer()) {
               sql = "with modelgroup(group_id,group_name,parent_id,tree_code_num) as(select group_id,group_name,parent_id,tree_code_num from model_group where group_name like '%"
                  + modelGroupName
                  + "%'"
                  + escapeStr
                  + " and is_topic = 0 and datasource = '"
                  + dataSource
                  + "' and group_id in ("
                  + modelGroups
                  + ")"
                  + " union all "
                  + "select k.group_id,k.group_name,k.parent_id,k.tree_code_num from model_group k "
                  + "inner join modelgroup on k.parent_id = modelgroup.group_id) "
                  + "select group_id,group_name,parent_id,tree_code_num from modelgroup order by parent_id";
            } else {
               if (DBSelectUtil.isMysql()) {
                  List<Object[]> modelGroupList = new ArrayList<>();
                  List<Object> h = new ArrayList<>();

                  for (int i = 0; i < groupIds.size(); i++) {
                     String rec = "SELECT group_id,group_name,parent_id,tree_code_num from model_group where datasource = '"
                        + dataSource
                        + "' and group_name like '%"
                        + modelGroupName
                        + "%'"
                        + escapeStr
                        + " and t.is_topic = 0 and find_in_set(group_id,getChildList("
                        + groupIds.get(i)
                        + ")) order by parent_id";
                     Query query = session.createSQLQuery(rec);
                     List<Object[]> result = query.list();
                     if (result != null && result.size() > 0) {
                        for (int j = 0; j < result.size(); j++) {
                           if (!h.contains(result.get(j)[0])) {
                              modelGroupList.add(result.get(j));
                              h.add(result.get(j)[0]);
                           }
                        }
                     }
                  }

                  return modelGroupList;
               }

               sql = "select group_id,group_name,parent_id,tree_code_num from model_group t where t.datasource = '"
                  + dataSource
                  + "' "
                  + " start with t.group_name like '%"
                  + modelGroupName
                  + "%'"
                  + escapeStr
                  + " and t.is_topic = 0 and t.group_id IN ("
                  + modelGroups
                  + ")"
                  + " connect by prior t.group_id = t.parent_id order by t.parent_id ";
            }
         } else if (DBSelectUtil.isSqlServer()) {
            sql = "with modelgroup(group_id,group_name,parent_id,tree_code_num) as(select group_id,group_name,parent_id,tree_code_num from model_group where datasource = '"
               + dataSource
               + "' group_id in ("
               + modelGroups
               + ")"
               + " union all "
               + "select k.group_id,k.group_name,k.parent_id,k.tree_code_num from model_group k "
               + "inner join modelgroup on k.parent_id = modelgroup.group_id) "
               + "select group_id,group_name,parent_id,tree_code_num from modelgroup order by parent_id";
         } else {
            if (DBSelectUtil.isMysql()) {
               List<Object[]> modelGroupList = new ArrayList<>();
               List<Object> h = new ArrayList<>();

               for (int ix = 0; ix < groupIds.size(); ix++) {
                  String rec = "SELECT group_id,group_name,parent_id,tree_code_num from model_group where datasource = '"
                     + dataSource
                     + "' and t.is_topic = 0 and find_in_set(group_id,getChildList("
                     + groupIds.get(ix)
                     + ")) order by parent_id";
                  Query query = session.createSQLQuery(rec);
                  List<Object[]> result = query.list();
                  if (result != null && result.size() > 0) {
                     for (int jx = 0; jx < result.size(); jx++) {
                        if (!h.contains(result.get(jx)[0])) {
                           modelGroupList.add(result.get(jx));
                           h.add(result.get(jx)[0]);
                        }
                     }
                  }
               }

               return modelGroupList;
            }

            sql = "SELECT group_id,group_name,parent_id,tree_code_num from model_group where datasource = '"
               + dataSource
               + "' start with group_id IN ("
               + modelGroups
               + ")  "
               + " connect by prior group_id = parent_id order by parent_id";
         }

         if (DBSelectUtil.isMysql()) {
            return null;
         } else {
            Query query = session.createSQLQuery(sql);
            return query.list();
         }
      } catch (Exception var19) {
         logger.error("查询所有模型组错误", var19);
         return null;
      } finally {
         if (session != null) {
            session.close();
         }
      }
   }

   public List<ModelGroup> queryAllModelGroupsByPermission(List<Long> modelGroupIds, String dataSource) {
      List<ModelGroup> modelGroups = new ArrayList<>();
      Session session = null;

      Object var21;
      try {
         StringBuffer sb = this.splicingModelGroupIds(
            modelGroupIds != null && modelGroupIds.size() > 0 ? this.removeDuplicate(modelGroupIds) : modelGroupIds, "group_id"
         );
         String sql = "";
         session = this.hiberTemp.getSessionFactory().openSession();
         if (DBSelectUtil.isSqlServer()) {
            sql = "with modelgroup(group_id,group_name,parent_id) as(select group_id,group_name,parent_id from model_group where group_id IN ("
               + sb.toString()
               + ") and datasource = '"
               + dataSource
               + "' union all select k.group_id,k.group_name,k.parent_id from model_group k "
               + "inner join modelgroup on k.parent_id = modelgroup.group_id where k.datasource = '"
               + dataSource
               + "') "
               + "select group_id,group_name,parent_id from modelgroup order by group_id";
         } else if (!DBSelectUtil.isMysql()) {
            sql = "SELECT group_id,group_name,parent_id from model_group where datasource = '"
               + dataSource
               + "' start with group_id IN ("
               + sb.toString()
               + ") "
               + "connect by prior group_id = parent_id order by group_id";
         } else {
            List<Object> h = new ArrayList<>();

            for (int i = 0; i < modelGroupIds.size(); i++) {
               String rec = "SELECT group_id,group_name,parent_id from model_group where datasource = '"
                  + dataSource
                  + "' and is_topic = 0 and find_in_set(group_id,getChildList("
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

         var21 = modelGroups;
      } catch (Exception var17) {
         logger.error("查询权限系统下的所有模型组错误", var17);
         return modelGroups;
      } finally {
         if (session != null) {
            session.close();
         }
      }

      return (List<ModelGroup>)var21;
   }

   public HibernateTemplateUtils getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
