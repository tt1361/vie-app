package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.model.ModelGroup;
import com.iflytek.vie.app.pojo.model.ModelInfo;
import com.iflytek.vie.app.provider.common.SqlHelper;
import com.iflytek.vie.app.provider.pojo.ModelStatus;
import com.iflytek.vie.utils.DateUtils;
import com.iflytek.vie.utils.ExceptionUtil;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate4.HibernateCallback;

public class ModelDB {
   private static final Logger logger = LoggerFactory.getLogger(ModelDB.class);
   private HibernateTemplateUtils hiberTemp;
   private ModelGroupDB modelGroupDB;

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }

   public void setModelGroupDB(ModelGroupDB modelGroupDB) {
      this.modelGroupDB = modelGroupDB;
   }

   public Long saveModelAndGetModelID(ModelInfo modelInfo) throws Exception {
      return (Long)this.hiberTemp.save(modelInfo);
   }

   public ModelInfo queryModelById(Long modelId) {
      List<ModelInfo> modelInfoList = (List<ModelInfo>)(List<?>)this.hiberTemp.findByNamedQuery("QueryModelById", new Object[]{modelId});
      return modelInfoList != null && modelInfoList.size() > 0 ? modelInfoList.get(0) : new ModelInfo();
   }

   public long saveModel(ModelInfo modelInfo) {
      return (Long)this.hiberTemp.save(modelInfo);
   }

   public Object[] queryModelInfoById(long modelId) {
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append("SELECT mi.model_id,mi.model_name,mi.group_id,mi.update_time,")
         .append("mi.modifier_id,mi.modifier_name,mi.datasource,mi.filter_rule,")
         .append("task.task_status,mi.model_hitcount,mi.is_up,mi.total_count,mi.model_comment,")
         .append("mi.model_accuracy  FROM MODEL_INFO mi ")
         .append(" LEFT JOIN (SELECT mt.model_id,ti.task_status FROM MODEL_TASK_T mt LEFT JOIN TASK_INFO_T ti")
         .append(" ON mt.task_id=ti.task_id WHERE ti.run_schema=0) task ON mi.model_id=task.model_id")
         .append(" WHERE mi.model_id= :modelId AND mi.model_status!=2 ");

      try {
         return (Object[])this.hiberTemp.fetchSQLFirstResult(sqlBuffer.toString(), new String[]{"modelId"}, new Object[]{modelId});
      } catch (Exception var5) {
         logger.error("queryModelInfoById DB has a error!", var5);
         return null;
      }
   }

   public List<ModelInfo> queryModelByIds(List<Long> modelIdList) {
      try {
         if (modelIdList != null && modelIdList.size() > 0) {
            modelIdList = this.removeDuplicate(modelIdList);
         }

         StringBuffer sb = this.splicingModelGroupIds(modelIdList, "MF.modelId");
         String hql = "SELECT MF FROM ModelInfo as MF WHERE MF.modelId IN ( " + sb.toString() + " ) AND MF.modelStatus=0";
         return (List)this.hiberTemp.createQuery(hql, new Object[0]);
      } catch (Exception var5) {
         logger.error("queryModelByIds DB has a error!", var5);
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

   public List<ModelInfo> queryAllModels(String dataSource, ModelStatus modelStatus, List<Long> groupIds) {
      if (groupIds != null && groupIds.size() != 0) {
         String sql = SqlHelper.queryAllOnLineModels(dataSource, groupIds);
         Map<Long, ModelGroup> modelGroupMap = this.fillGroupInfo(groupIds, dataSource);
         List<String> paramNames = new ArrayList<>();
         paramNames.add("dataSource");
         List<Object> values = new ArrayList<>();
         values.add(dataSource);
         List<Object[]> list = (List<Object[]>)(List<?>)this.hiberTemp.createSQLQuery(sql, paramNames, values);
         List<ModelInfo> modelInfos = new ArrayList<>();
         if (list != null && list.size() != 0) {
            for (Object[] objects : list) {
               ModelInfo modelInfo = new ModelInfo();
               modelInfo.setModelId(Long.parseLong(String.valueOf(objects[0])));
               modelInfo.setModelName(String.valueOf(objects[1]));
               modelInfo.setModelGroup(modelGroupMap.get(Long.parseLong(String.valueOf(objects[2]))));
               modelInfos.add(modelInfo);
            }

            return modelInfos;
         } else {
            return modelInfos;
         }
      } else {
         return new ArrayList<>();
      }
   }

   public HashMap<Integer, String> queryModelOnline(String modelName, List<Long> groupIds, String dataSource) throws Exception {
      HashMap<Integer, String> allModelList = new HashMap<>();
      Session session = null;

      HashMap sb;
      try {
         StringBuffer sql = new StringBuffer();
         if (groupIds != null && groupIds.size() != 0) {
            groupIds = this.removeDuplicate(groupIds);
            StringBuffer sbx = this.splicingModelGroupIds(groupIds, "mi.group_id");
            sql.append(
               "SELECT mi.model_id,mi.model_name,mi.group_id  FROM MODEL_INFO mi LEFT JOIN MODEL_TASK_T mt ON mi.model_id=mt.model_id LEFT JOIN TASK_INFO_T ti ON mt.task_id=ti.task_id WHERE ti.run_schema=0 AND mi.dataSource='"
                  + dataSource
                  + "' AND mi.model_status!=2 AND ti.task_status=1"
            );
            sql.append(" AND mi.group_id IN ( " + sbx.toString() + " ) ");
            if (modelName != null && !"".equals(modelName)) {
               sql.append(" AND mi.model_name like '%" + modelName + "%'");
            }

            session = this.hiberTemp.getSessionFactory().openSession();
            List<Object[]> modelList = (List<Object[]>)(List<?>)session.createSQLQuery(sql.toString()).list();
            if (modelList != null && modelList.size() > 0) {
               for (Object[] modelInfo : modelList) {
                  allModelList.put(Integer.parseInt(modelInfo[0].toString()), String.valueOf(modelInfo[1]));
               }
            }

            return allModelList;
         }

         sb = allModelList;
      } catch (Exception var14) {
         logger.error("根据模型id查找模型名称", var14);
         return allModelList;
      } finally {
         session.close();
      }

      return sb;
   }

   public String queryModelByModelNameLike(String modelName) {
      Session session = null;
      String idsStr = "";

      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String sql = "SELECT mi.model_id   FROM MODEL_INFO mi   LEFT JOIN MODEL_TASK_T mt     ON mi.model_id = mt.model_id   LEFT JOIN TASK_INFO_T ti     ON mt.task_id = ti.task_id  WHERE ti.run_schema = 0    AND mi.model_status != 2    AND ti.task_status = 1 AND mi.model_name LIKE :modelName";
         Query query = session.createSQLQuery(sql).setParameter("modelName", "%" + modelName + "%");
         List<Object> data = query.list();
         if (data != null && data.size() > 0) {
            for (Object idstr : data) {
               idsStr = idsStr + String.valueOf(idstr) + ",";
            }

            idsStr = idsStr.substring(0, idsStr.length() - 1);
         }
      } catch (Exception var12) {
         logger.error("根据模型组查询模型", var12);
      } finally {
         session.close();
      }

      return idsStr;
   }

   public Map<Long, ModelGroup> fillGroupInfo(List<Long> groupIds, String dataSource) {
      Map<Long, ModelGroup> modelGroupMap = new HashMap<>();
      if (groupIds != null && groupIds.size() != 0) {
         for (ModelGroup modelGroup : this.modelGroupDB.queryAllModelGroupsByPermission(groupIds, dataSource)) {
            modelGroupMap.put(modelGroup.getGroupId(), modelGroup);
         }

         return modelGroupMap;
      } else {
         return modelGroupMap;
      }
   }

   public Object[] queryModelInfoByStatus(long modelId) {
      String sql = SqlHelper.queryModelInfoByStatus(modelId);

      try {
         return (Object[])this.hiberTemp.fetchSQLFirstResult(sql, new String[]{"modelId"}, new Object[]{modelId});
      } catch (Exception var5) {
         logger.error("queryModelInfoByStatus DB has a error!", var5);
         return null;
      }
   }

   public void modelOffline(final String id) {
      this.hiberTemp
         .execute(
            new HibernateCallback<Object>() {
               public Object doInHibernate(Session session) throws HibernateException {
                  String selectSql = "select ti.task_id,ti.task_name from model_task_t mt inner join task_info_t ti on mt.task_id=ti.task_id where mt.model_id="
                     + id;
                  List<Object[]> lists = (List<Object[]>)(List<?>)session.createSQLQuery(selectSql).list();
                  String updateFilterSql = "update ModelInfo set filterRule=null,modelHitCount=null,totalCount=null where modelId=" + id;
                  session.createQuery(updateFilterSql).executeUpdate();
                  if (lists != null && lists.size() > 0) {
                     for (Object[] objects : lists) {
                        Long taskId = Long.valueOf(objects[0].toString());
                        if (taskId != null) {
                           String updateSql = "update task_info_t set task_status=:task_status, update_time=:update_time where task_id = :task_id";
                           session.createSQLQuery(updateSql)
                              .setLong("task_status", SqlHelper.TASK_STATUS_DELETE)
                              .setString("update_time", DateUtils.parseDateToString(new Date(), "yyyy-MM-dd"))
                              .setLong("task_id", taskId)
                              .executeUpdate();
                        }

                        if (id != null && taskId != null) {
                           String delSql = "delete model_task_t  where model_id=" + id + " and task_id=" + taskId;
                           session.createSQLQuery(delSql).executeUpdate();
                        }
                     }

                     return null;
                  } else {
                     return null;
                  }
               }
            }
         );
   }

   public List<Object[]> queryModelByGroupIds2(List<Long> modelIdList, List<Long> groupIds) {
      if (groupIds != null && groupIds.size() != 0 && modelIdList != null && modelIdList.size() != 0) {
         try {
            StringBuffer sbf = new StringBuffer();
            sbf.append("select t.model_id,t.model_name,t.TEXT_RULE from model_info t ")
               .append("inner join model_task_t mtt on t.model_id=mtt.model_id ")
               .append("inner join task_info_t tt on mtt.task_id=tt.task_id ")
               .append(" inner join model_group mg on t.group_id=mg.group_id  where mg.group_id in (:groupIds)")
               .append(" and  t.model_status=0 and tt.task_status=1 and t.model_id in (:modelIdList)");
            String[] params = new String[]{"groupIds", "modelIdList"};
            Object[] values = new Object[]{groupIds, modelIdList};
            return (List)this.hiberTemp.createSQLQuery(sbf.toString(), params, values);
         } catch (Exception var7) {
            logger.error("queryModelByGroupIds2 DB has a error!", var7);
            return null;
         }
      } else {
         return null;
      }
   }

   public List<Object[]> querySqlModelByID(List<String> modelIds) {
      try {
         String sql = SqlHelper.queryModelByID(modelIds);
         return (List)this.hiberTemp.createSQLQuery(sql, new String[]{"modelIds"}, new Object[]{modelIds});
      } catch (Exception var3) {
         throw new RuntimeException(ExceptionUtil.getStackTrace(var3));
      }
   }
}
