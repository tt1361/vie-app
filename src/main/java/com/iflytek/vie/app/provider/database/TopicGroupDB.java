package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.topicgroup.TopicGroup;
import com.iflytek.vie.app.pojo.topicgroup.TopicGroupModel;
import com.iflytek.vie.utils.DBSelectUtil;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicGroupDB {
   private static final Logger logger = LoggerFactory.getLogger(TopicGroupDB.class);
   private HibernateTemplateUtils hiberTemp;

   public List<Object[]> getParentGroupByType(int topicGroupType, List<String> seatGroupList, int settingPage) {
      List<Object[]> resultList = new ArrayList<>();
      String sql = "";
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      if (topicGroupType != 1 || settingPage != 0) {
         sql = " select distinct group_id, group_name from topic_group  where group_type = :topicGroupType and deleted = 0 and parent_group_id = 0  order by group_id";
         paramNames.add("topicGroupType");
         values.add(topicGroupType);
      } else if (DBSelectUtil.isSqlServer()) {
         sql = sql
            + "with topicgroup(group_id,group_name,parent_group_id,deleted) as (select group_id,group_name,parent_group_id,deleted from topic_group where group_name IN (:seatGroupList) and group_type = :topicGroupType and deleted = 0 union all  select k.group_id,k.group_name,k.parent_group_id,k.deleted from topic_group k inner join topicgroup on k.group_id = topicgroup.parent_group_id)select distinct group_id, group_name from topicgroup where parent_group_id = 0 and deleted = 0 order by group_id";
         paramNames.add("seatGroupList");
         values.add(seatGroupList);
         paramNames.add("topicGroupType");
         values.add(topicGroupType);
      } else {
         sql = " select distinct group_id, group_name from topic_group  where group_type = :topicGroupType and deleted = 0 and parent_group_id = 0  start with group_name in (:seatGroupList) connect by prior parent_group_id = group_id  order by group_id";
         paramNames.add("topicGroupType");
         values.add(topicGroupType);
         paramNames.add("seatGroupList");
         values.add(seatGroupList);
      }

      try {
         resultList = (List)this.hiberTemp.createSQLQuery(sql, paramNames, values);
      } catch (Exception var9) {
         logger.error("getParentGroupByType DB has a error!", var9);
      }

      return resultList;
   }

   public List<Object[]> getGroupModelList(Long groupId, List<String> seatGroupList) {
      List<Object[]> resultList = new ArrayList<>();
      String sql = "";
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      if (DBSelectUtil.isSqlServer()) {
         sql = "with topicgroup(group_id,parent_group_id) as(select group_id,parent_group_id from topic_group where group_id = :groupId union all select k.group_id,k.parent_group_id from topic_group k inner join topicgroup on k.parent_group_id = topicgroup.group_id)select distinct t1.group_id,t1.group_name,t3.model_id from topic_group t1 left join topic_group_model t2 on t1.group_id = t2.topic_group_id left join model_info t3 on t2.model_id = t3.model_id left join model_task_t t4 on t3.model_id = t4.model_id left join task_info_t t5 on t4.task_id = t5.task_id where t1.group_type = 1 and t1.deleted = 0 and t3.model_status = 0 and t5.task_status = 1 and t1.group_name in (:seatGroupList)  and t1.group_id in (select distinct group_id from topicgroup) order by t1.group_id";
         paramNames.add("groupId");
         values.add(groupId);
         paramNames.add("seatGroupList");
         values.add(seatGroupList);
      } else {
         sql = "select distinct t1.group_id,t1.group_name,t3.model_id from topic_group t1 left join topic_group_model t2 on t1.group_id = t2.topic_group_id left join model_info t3 on t2.model_id = t3.model_id left join model_task_t t4 on t3.model_id = t4.model_id left join task_info_t t5 on t4.task_id = t5.task_id where t1.group_type = 1 and t1.deleted = 0 and t3.model_status = 0 and t5.task_status = 1 and t1.group_name in (:seatGroupList)  start with t1.group_id = :groupId connect by prior t1.group_id= t1.parent_group_id order by t1.group_id";
         paramNames.add("seatGroupList");
         values.add(seatGroupList);
         paramNames.add("groupId");
         values.add(groupId);
      }

      try {
         resultList = (List)this.hiberTemp.createSQLQuery(sql, paramNames, values);
      } catch (Exception var8) {
         logger.error("getGroupModelList DB has a error!", var8);
      }

      return resultList;
   }

   public List<BigDecimal> getModelIdList(Long groupId, int topicGroupType) {
      List<BigDecimal> resultList = new ArrayList<>();
      String sql = "";
      if (DBSelectUtil.isSqlServer()) {
         sql = "with topicgroup(group_id,parent_group_id) as(select group_id,parent_group_id from topic_group where group_id = :groupId union all select k.group_id,k.parent_group_id from topic_group k inner join topicgroup on k.parent_group_id = topicgroup.group_id)select distinct t3.model_id from topic_group t1 left join topic_group_model t2 on t1.group_id = t2.topic_group_id left join model_info t3 on t2.model_id = t3.model_id left join model_task_t t4 on t3.model_id = t4.model_id left join task_info_t t5 on t4.task_id = t5.task_id where t1.group_type = :topicGroupType and t1.deleted = 0 and t3.model_status = 0  and t5.task_status = 1 and t1.group_id in (select group_id from topicgroup)";
      } else {
         sql = "select distinct t3.model_id from topic_group t1 left join topic_group_model t2 on t1.group_id = t2.topic_group_id left join model_info t3 on t2.model_id = t3.model_id left join model_task_t t4 on t3.model_id = t4.model_id left join task_info_t t5 on t4.task_id = t5.task_id where t1.group_type = :topicGroupType and t1.deleted = 0 and t3.model_status = 0  and t5.task_status = 1 start with t1.group_id = :groupId connect by prior t1.group_id= t1.parent_group_id";
      }

      try {
         if (DBSelectUtil.isSqlServer()) {
            resultList = (List)this.hiberTemp.createSQLQuery(sql, new String[]{"groupId", "topicGroupType"}, new Object[]{groupId, topicGroupType});
         } else {
            resultList = (List)this.hiberTemp.createSQLQuery(sql, new String[]{"topicGroupType", "groupId"}, new Object[]{topicGroupType, groupId});
         }
      } catch (Exception var6) {
         logger.error("getModelIdList DB has a error!", var6);
      }

      return resultList;
   }

   public TopicGroup getEntityById(Long groupId) {
      TopicGroup topicGroup = null;

      try {
         if (groupId == null) {
            return topicGroup;
         }

         topicGroup = (TopicGroup)this.hiberTemp.get(TopicGroup.class, groupId);
      } catch (Exception var4) {
         logger.error("getEntityById DB has a error!", var4);
      }

      return topicGroup;
   }

   public List<Object[]> getModelAndGroupIdList(Long groupId) {
      List<Object[]> resultList = new ArrayList<>();
      String sql = "";
      if (DBSelectUtil.isSqlServer()) {
         sql = "with topicgroup(group_id,parent_group_id) as(select group_id,parent_group_id from topic_group where group_id = :groupId union all select k.group_id,k.parent_group_id from topic_group k inner join topicgroup on k.parent_group_id = topicgroup.group_id)select distinct t3.model_id, t1.group_id from topic_group t1 left join topic_group_model t2 on t1.group_id = t2.topic_group_id left join model_info t3 on t2.model_id = t3.model_id left join model_task_t t4 on t3.model_id = t4.model_id left join task_info_t t5 on t4.task_id = t5.task_id where t1.group_type = 1 and t1.deleted = 0 and t3.model_status = 0 and t5.task_status = 1 and t1.group_id in (select distinct group_id from topicgroup) order by t3.model_id ";
      } else {
         sql = "select distinct t3.model_id, t1.group_id from topic_group t1 left join topic_group_model t2 on t1.group_id = t2.topic_group_id left join model_info t3 on t2.model_id = t3.model_id left join model_task_t t4 on t3.model_id = t4.model_id left join task_info_t t5 on t4.task_id = t5.task_id where t1.group_type = 1 and t1.deleted = 0 and t3.model_status = 0 and t5.task_status = 1 start with t1.group_id = :groupId connect by prior t1.group_id= t1.parent_group_id order by t3.model_id ";
      }

      try {
         resultList = (List)this.hiberTemp.createSQLQuery(sql, new String[]{"groupId"}, new Object[]{groupId});
      } catch (Exception var5) {
         logger.error("getModelAndGroupIdList DB has a error!", var5);
      }

      return resultList;
   }

   public List<Object[]> queryChildrenGroup(Long parentGroupId, int topicGroupType, List<String> seatGroupList, int isSetting) {
      List<Object[]> resultList = null;
      String sql = "";
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      if (topicGroupType != 1 || isSetting != 0) {
         sql = "select group_id, group_name from topic_group where deleted = 0 and parent_group_id = :parentGroupId and group_type = :topicGroupType order by group_id";
         paramNames.add("parentGroupId");
         values.add(parentGroupId);
         paramNames.add("topicGroupType");
         values.add(topicGroupType);
      } else if (DBSelectUtil.isSqlServer()) {
         sql = "with topicgroup(group_id,parent_group_id) as  (select group_id,parent_group_id from topic_group where group_name IN (:seatGroupList) and group_type = :topicGroupType and deleted = 0 union all select k.group_id,k.parent_group_id from topic_group k inner join topicgroup on k.group_id = topicgroup.parent_group_id)select group_id, group_name from topic_group where deleted = 0 and parent_group_id = :parentGroupId and group_id in (select distinct group_id from topicgroup) order by group_id";
         paramNames.add("seatGroupList");
         values.add(seatGroupList);
         paramNames.add("topicGroupType");
         values.add(topicGroupType);
         paramNames.add("parentGroupId");
         values.add(parentGroupId);
      } else {
         sql = "select group_id, group_name from topic_group where deleted = 0 and parent_group_id = :parentGroupId and group_id in (select distinct group_id from topic_group where group_type = :topicGroupType and deleted = 0 start with group_name in (:seatGroupList) connect by prior parent_group_id = group_id) order by group_id";
         paramNames.add("parentGroupId");
         values.add(parentGroupId);
         paramNames.add("topicGroupType");
         values.add(topicGroupType);
         paramNames.add("seatGroupList");
         values.add(seatGroupList);
      }

      try {
         resultList = (List)this.hiberTemp.createSQLQuery(sql, paramNames, values);
      } catch (Exception var10) {
         logger.error("queryChildrenGroup DB has a error!", var10);
      }

      return resultList;
   }

   public List<Object[]> getChildrenModel(Long groupId) {
      List<Object[]> resultList = new ArrayList<>();
      String sql = "select t1.model_id, t1.model_name from model_info t1 join topic_group_model t2 on t1.model_id = t2.model_id join topic_group t3 on t2.topic_group_id = t3.group_id join model_task_t t4 on t1.model_id = t4.model_id join task_info_t t5 on t4.task_id = t5.task_id where t3.group_id = :groupId and t3.deleted = 0 and t1.model_status = 0 and t5.task_status = 1 order by t1.model_id";

      try {
         resultList = (List)this.hiberTemp.createSQLQuery(sql, new String[]{"groupId"}, new Object[]{groupId});
      } catch (Exception var5) {
         logger.error("getChildrenModel DB has a error!", var5);
      }

      return resultList;
   }

   public boolean isExistName(Long parentGroupId, String topicGroupName, int topicGroupType) {
      boolean flag = false;
      List<Long> resultList = null;

      try {
         if (topicGroupType == 1) {
            String hql = "select 0 from TopicGroup where groupName = :groupName and deleted = 0 and groupType = 1 ";
            resultList = (List)this.hiberTemp.createQuery(hql, new String[]{"groupName"}, new Object[]{topicGroupName});
         } else {
            String hql = "select 0 from TopicGroup where parentGroupId = :parentGroupId and groupName = :groupName and deleted = 0 and groupType = 0 ";
            resultList = (List)this.hiberTemp.createQuery(hql, new String[]{"parentGroupId", "groupName"}, new Object[]{parentGroupId, topicGroupName});
         }

         if (resultList != null && !resultList.isEmpty()) {
            flag = true;
         } else {
            flag = false;
         }
      } catch (Exception var7) {
         logger.error("isExistName DB has a error!", var7);
      }

      return flag;
   }

   public Long saveTopicGroup(TopicGroup topicGroup) {
      try {
         this.hiberTemp.save(topicGroup);
      } catch (Exception var3) {
         logger.error("saveTopicGroup DB has a error!", var3);
         return null;
      }

      return topicGroup.getGroupId();
   }

   public boolean updateTopicGroup(Long topicGroupId, String topicGroupName) {
      String sql = "update TopicGroup set groupName = :topicGroupName where groupId = :topicGroupId ";

      try {
         int code = this.hiberTemp.bulkUpdate(sql, new String[]{"topicGroupName", "topicGroupId"}, new Object[]{topicGroupName, topicGroupId});
         return code != 0;
      } catch (Exception var5) {
         logger.error("updateTopicGroup DB has a error!", var5);
         return false;
      }
   }

   public boolean deleteTopicGroup(Long topicGroupId) {
      String sql = "";
      if (DBSelectUtil.isSqlServer()) {
         sql = "with topicgroup(group_id,parent_group_id) as  (select group_id,parent_group_id from topic_group where group_id = :topicGroupId union all select k.group_id,k.parent_group_id from topic_group k inner join topicgroup on k.parent_group_id = topicgroup.group_id)update topic_group set deleted = 1 where group_id in (select group_id from topicgroup) ";
      } else {
         sql = "update topic_group set deleted = 1 where group_id in (select distinct group_id from topic_group connect by prior group_id = parent_group_id start with group_id = :topicGroupId)";
      }

      try {
         int code = this.hiberTemp.bulkSQLUpdate(sql, new String[]{"topicGroupId"}, new Object[]{topicGroupId});
         return code != 0;
      } catch (Exception var4) {
         logger.error("deleteTopicGroup DB has a error!", var4);
         return false;
      }
   }

   public void addGroupModels(List<TopicGroupModel> models) {
      try {
         for (TopicGroupModel topicGroupModel : models) {
            this.hiberTemp.saveOrUpdate(topicGroupModel);
         }

         this.hiberTemp.flush();
      } catch (Exception var4) {
         logger.error("addGroupModels DB has a error!", var4);
      }
   }

   public boolean deleteGroupModel(Long parentGroupId, Long modelId) {
      String sql = "delete from TopicGroupModel where topicGroupId = :topicGroupId and modelId = :modelId ";

      try {
         int code = this.hiberTemp.bulkUpdate(sql, new String[]{"topicGroupId", "modelId"}, new Object[]{parentGroupId, modelId});
         return code != 0;
      } catch (Exception var5) {
         logger.error("deleteGroupModel DB has a error!", var5);
         return false;
      }
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
