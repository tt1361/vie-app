package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.custom.CustomHomePage;
import com.iflytek.vie.app.pojo.custom.HomePageRelateModule;
import com.iflytek.vie.app.pojo.custom.PageRelateModuleRequest;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomHomePageDB {
   private static final Logger logger = LoggerFactory.getLogger(CustomHomePageDB.class);
   private HibernateTemplateUtils hiberTemp;

   public boolean checkPageNameExist(String userId, String pageName, Long pageId, String dataSource) {
      boolean flag = false;
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      List<CustomHomePage> pageList = null;

      try {
         String hql = "FROM CustomHomePage WHERE dataSource = :dataSource and createUser = :userId AND pageName = :pageName ";
         if (pageId != null) {
            hql = hql + "AND id != :pageId ";
            paramNames.add("pageId");
            values.add(pageId);
         }

         paramNames.add("dataSource");
         values.add(dataSource);
         paramNames.add("userId");
         values.add(userId);
         paramNames.add("pageName");
         values.add(pageName);
         pageList = (List)this.hiberTemp.createQuery(hql, paramNames, values);
         if (pageList != null && pageList.size() > 0) {
            flag = true;
         }
      } catch (Exception var10) {
         logger.error("checkPageNameExist DB has a error!", var10);
      }

      return flag;
   }

   public boolean checkIsNotExist(Long id) {
      boolean flag = false;

      try {
         String sql = "select t.topic_id from topic_info t where t.topic_status!=1 and t.topic_id=" + id;
         List list = (List)this.hiberTemp.createSQLQuery(sql, new Object[0]);
         if (list != null && list.isEmpty()) {
            flag = true;
         }
      } catch (Exception var5) {
         logger.error("检查专题或者报表是否被删除出错!", var5);
      }

      return flag;
   }

   public boolean checkPageNumOverLimit(Integer maxNum, String userId, String dataSource) {
      boolean flag = false;

      try {
         String sql = "SELECT COUNT(1) FROM CUSTOM_HOMEPAGE WHERE dataSource = :dataSource and create_user = :userId ";
         Object result = this.hiberTemp.uniqueSQLResult(sql, new String[]{"dataSource", "userId"}, new Object[]{dataSource, userId});
         int totalNum = result == null ? 0 : Integer.parseInt(String.valueOf(result));
         if (totalNum >= maxNum) {
            flag = true;
         }

         return flag;
      } catch (Exception var8) {
         logger.error("checkPageNumOverLimit DB has a error!", var8);
         return flag;
      }
   }

   public Long saveCustomHomePage(CustomHomePage homePage) {
      try {
         this.hiberTemp.save(homePage);
      } catch (Exception var3) {
         logger.error("saveCustomHomePage DB has a error!", var3);
         return null;
      }

      return homePage.getId();
   }

   public boolean updateCustomHomePage(CustomHomePage homePage) {
      boolean flag = true;

      try {
         CustomHomePage homeBean = (CustomHomePage)this.hiberTemp.get(CustomHomePage.class, homePage.getId());
         if (homeBean != null) {
            homeBean.setPageName(homePage.getPageName());
         } else {
            flag = false;
         }

         return flag;
      } catch (Exception var4) {
         logger.error("updateCustomHomePage DB has a error!", var4);
         return false;
      }
   }

   public boolean deleteCustomHomePage(Long pageId) {
      boolean flag = true;

      try {
         String deleteModuleHql = "DELETE FROM HomePageRelateModule WHERE pageId = :pageId ";
         this.hiberTemp.bulkUpdate(deleteModuleHql, new String[]{"pageId"}, new Object[]{pageId});
         String deletePagehql = "DELETE FROM CustomHomePage WHERE id = :pageId ";
         int code = this.hiberTemp.bulkUpdate(deletePagehql, new String[]{"pageId"}, new Object[]{pageId});
         if (code == 0) {
            flag = false;
         } else {
            flag = true;
         }

         return flag;
      } catch (Exception var6) {
         logger.error("deleteCustomHomePage DB has a error!", var6);
         return false;
      }
   }

   public List<CustomHomePage> queryCustomHomePageList(String userId, String dataSource) {
      List<CustomHomePage> pageList = null;

      try {
         String hql = "FROM CustomHomePage WHERE dataSource = :dataSource and createUser = :userId ORDER BY id ASC ";
         return (List)this.hiberTemp.createQuery(hql, new String[]{"dataSource", "userId"}, new Object[]{dataSource, userId});
      } catch (Exception var5) {
         logger.error("queryCustomHomePageList DB has a error!", var5);
         return pageList;
      }
   }

   public boolean checkModuleNameExist(String userId, String moduleName, Long id, Long pageId) {
      boolean flag = false;
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      List<HomePageRelateModule> pageList = null;

      try {
         String hql = "FROM HomePageRelateModule WHERE createUser = :userId AND moduleName = :moduleName ";
         if (id != null) {
            hql = hql + "AND id != :id ";
            paramNames.add("id");
            values.add(id);
         }

         hql = hql + "AND pageId = :pageId ";
         paramNames.add("userId");
         values.add(userId);
         paramNames.add("moduleName");
         values.add(moduleName);
         paramNames.add("pageId");
         values.add(pageId);
         pageList = (List)this.hiberTemp.createQuery(hql, paramNames, values);
         if (pageList != null && pageList.size() > 0) {
            flag = true;
         }
      } catch (Exception var10) {
         logger.error("checkModuleNameExist DB has a error!", var10);
      }

      return flag;
   }

   public boolean checkPageModuleOverLimit(Integer maxNum, Long pageId, Long moduleId) {
      boolean flag = false;
      List<String> params = new ArrayList<>();
      List<Object> values = new ArrayList<>();

      try {
         String sql = "SELECT COUNT(1) FROM HOMEPAGE_RELATE_MODULE WHERE PAGE_ID = :pageId ";
         params.add("pageId");
         values.add(pageId);
         if (moduleId != null) {
            sql = sql + "AND ID != :moduleId ";
            params.add("moduleId");
            values.add(moduleId);
         }

         Object result = this.hiberTemp.uniqueSQLResult(sql, params, values);
         int totalNum = result == null ? 0 : Integer.parseInt(String.valueOf(result));
         if (totalNum >= maxNum) {
            flag = true;
         }

         return flag;
      } catch (Exception var10) {
         logger.error("checkPageModuleOverLimit DB has a error!", var10);
         return flag;
      }
   }

   public boolean savePageRelateModule(HomePageRelateModule pageRelateModule) {
      boolean flag = true;

      try {
         this.hiberTemp.save(pageRelateModule);
         return flag;
      } catch (Exception var4) {
         logger.error("savePageRelateModule DB has a error!", var4);
         return false;
      }
   }

   public boolean updatePageRelateModule(HomePageRelateModule pageRelateModule) {
      boolean flag = true;

      try {
         HomePageRelateModule moduleBean = (HomePageRelateModule)this.hiberTemp.get(HomePageRelateModule.class, pageRelateModule.getId());
         if (moduleBean != null) {
            moduleBean.setModuleName(pageRelateModule.getModuleName());
            moduleBean.setPageId(pageRelateModule.getPageId());
            moduleBean.setContentParam(pageRelateModule.getContentParam());
            if (pageRelateModule.getContentType() != null) {
               moduleBean.setContentType(pageRelateModule.getContentType());
            }
         } else {
            flag = false;
         }

         return flag;
      } catch (Exception var4) {
         logger.error("updatePageRelateModule DB has a error!", var4);
         return false;
      }
   }

   public boolean deletePageRelateModule(Long id) {
      boolean flag = true;

      try {
         String hql = "DELETE FROM HomePageRelateModule WHERE id = :id ";
         int code = this.hiberTemp.bulkUpdate(hql, new String[]{"id"}, new Object[]{id});
         if (code == 0) {
            flag = false;
         } else {
            flag = true;
         }

         return flag;
      } catch (Exception var5) {
         logger.error("deletePageRelateModule DB has a error!", var5);
         return false;
      }
   }

   public boolean deleteModuleFromHomePage(PageRelateModuleRequest pageRelateModule) {
      boolean flag = true;
      List<Object> values = new ArrayList<>();

      try {
         StringBuffer sql = new StringBuffer();
         sql.append("DELETE FROM Homepage_Relate_Module WHERE 1=1 ");
         if (pageRelateModule.getModuleType() != null) {
            sql.append("AND MODULE_TYPE = ? ");
            values.add(pageRelateModule.getModuleType());
         }

         if (!StringUtils.isNullOrEmpry(pageRelateModule.getModuleIds())) {
            sql.append("AND MODULE_ID in (" + pageRelateModule.getModuleIds() + ")");
         }

         if (!StringUtils.isNullOrEmpry(pageRelateModule.getModuleDetailInfo())) {
            sql.append("AND MODULE_DETAIL_INFO = ? ");
            values.add(pageRelateModule.getModuleDetailInfo());
         }

         int code = this.hiberTemp.bulkSQLUpdate(sql.toString(), values);
         if (code == 0) {
            flag = false;
         } else {
            flag = true;
         }

         return flag;
      } catch (Exception var6) {
         logger.error("deleteModuleFromHomePage DB has a error!", var6);
         return false;
      }
   }

   public CustomHomePage queryCustomPageById(Long pageId) {
      CustomHomePage pageInfo = null;

      try {
         return (CustomHomePage)this.hiberTemp.get(CustomHomePage.class, pageId);
      } catch (Exception var4) {
         logger.error("queryCustomPageById DB has a error!", var4);
         return pageInfo;
      }
   }

   public List<HomePageRelateModule> queryPageRelateModuleList(Long pageId) {
      List<HomePageRelateModule> pageList = null;

      try {
         String hql = "FROM HomePageRelateModule WHERE pageId = :pageId ORDER BY id ASC ";
         return (List)this.hiberTemp.createQuery(hql, new String[]{"pageId"}, new Object[]{pageId});
      } catch (Exception var4) {
         logger.error("queryPageRelateModuleList DB has a error!", var4);
         return pageList;
      }
   }

   public HomePageRelateModule queryModuleInfoById(Long id) {
      HomePageRelateModule moduleInfo = null;

      try {
         String hql = "SELECT m, n.pageName FROM HomePageRelateModule m, CustomHomePage n WHERE m.pageId = n.id AND m.id = :id ";
         Object[] obj = (Object[])this.hiberTemp.uniqueResult(hql, new String[]{"id"}, new Object[]{id});
         if (obj != null && obj.length == 2) {
            moduleInfo = (HomePageRelateModule)obj[0];
            moduleInfo.setPageName(String.valueOf(obj[1]));
         }

         return moduleInfo;
      } catch (Exception var5) {
         logger.error("queryModuleInfoById DB has a error!", var5);
         return moduleInfo;
      }
   }

   public long queryModuleIfSendHomePage(PageRelateModuleRequest pageRelateModuleRequest) {
      List<HomePageRelateModule> pageList = null;
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();

      try {
         String hql = "FROM HomePageRelateModule WHERE 1=1 AND moduleType = :moduleType AND moduleId = :moduleId AND createUser = :userId ";
         if (!StringUtils.isNullOrEmpry(pageRelateModuleRequest.getModuleDetailInfo())) {
            hql = hql + "AND moduleDetailInfo = :moduleDetailInfo ";
            paramNames.add("moduleDetailInfo");
            values.add(pageRelateModuleRequest.getModuleDetailInfo());
         }

         paramNames.add("moduleType");
         values.add(pageRelateModuleRequest.getModuleType());
         paramNames.add("moduleId");
         values.add(pageRelateModuleRequest.getModuleId());
         paramNames.add("userId");
         values.add(pageRelateModuleRequest.getUserId());
         pageList = (List)this.hiberTemp.createQuery(hql, paramNames, values);
         return pageList != null && pageList.size() > 0 ? pageList.get(0).getId() : 0L;
      } catch (Exception var6) {
         logger.error("queryModuleIfSendHomePage DB has a error!", var6);
         return 0L;
      }
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
