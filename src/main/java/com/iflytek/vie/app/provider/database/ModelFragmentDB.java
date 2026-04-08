package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.model.ModelFragment;
import com.iflytek.vie.utils.ListUtil;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate4.HibernateTemplate;

public class ModelFragmentDB {
   private HibernateTemplate hiberTemp;
   private final Logger logger = LoggerFactory.getLogger(ModelFragmentDB.class);

   public ModelFragment searchModelFragmentById(long fragmentId) {
      Session session = null;

      Object var7;
      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String hql = "SELECT T FROM ModelFragment T WHERE T.fragmentId = ?";
         Query query = session.createQuery(hql).setParameter(0, fragmentId);
         List<ModelFragment> fragmentList = query.list();
         if (!ListUtil.isNullOrEmpty(fragmentList)) {
            return fragmentList.get(0);
         }

         var7 = null;
      } catch (Exception var11) {
         this.logger.error("查询模型片段", var11);
         return null;
      } finally {
         if (session != null) {
            session.close();
         }
      }

      return (ModelFragment)var7;
   }

   public void saveList(List<ModelFragment> mfList) {
      Session session = null;

      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         session.beginTransaction();
         ModelFragment modelFragment = null;

         for (int i = 0; i < mfList.size(); i++) {
            modelFragment = mfList.get(i);
            session.saveOrUpdate(modelFragment);
            if (i == mfList.size() - 1) {
               session.flush();
               session.clear();
            } else if (i % 10 == 0) {
               session.flush();
               session.clear();
            }
         }

         session.getTransaction().commit();
      } catch (Exception var8) {
         this.logger.error("批量保存模型片段异常", var8);
      } finally {
         session.close();
      }
   }

   public List<Long> queryAllFragmentId(long modelId) {
      Session session = null;

      Object i$;
      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String sql = "SELECT t.fragmentId FROM ModelFragment t WHERE t.modelId=?";
         Query query = session.createQuery(sql).setParameter(0, modelId);
         List<Object> list = query.list();
         List<Long> ids = new ArrayList<>();
         if (list != null && list.size() != 0) {
            for (Object object : list) {
               ids.add(Long.parseLong(String.valueOf(object)));
            }

            return ids;
         }

         i$ = ids;
      } catch (Exception var13) {
         this.logger.error("查询所有片段ID异常", var13);
         return null;
      } finally {
         session.close();
      }

      return (List<Long>)i$;
   }

   public void deleteFragments(List<Long> fragmentIds) {
      Session session = null;

      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String hql = "DELETE FROM ModelFragment WHERE fragmentId in (:ids)";
         session.createQuery(hql).setParameterList("ids", fragmentIds).executeUpdate();
      } catch (Exception var7) {
         this.logger.error("删除片段异常", var7);
      } finally {
         session.close();
      }
   }

   public List<ModelFragment> getFragmentByIds(List<Long> id) {
      Session session = null;

      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String hql = "SELECT MF FROM ModelFragment MF WHERE MF.fragmentId in (:ids)";
         Query query = session.createQuery(hql).setParameterList("ids", id);
         return query.list();
      } catch (Exception var10) {
         this.logger.error("批量查询模型片段异常", var10);
      } finally {
         session.close();
      }

      return null;
   }

   public void deleteModelFragmentAnd(long modelId, long previewId) {
      Session session = null;

      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         session.beginTransaction();
         String hql = "DELETE FROM ModelFragment WHERE previewId=? AND modelId=?";
         session.createQuery(hql).setParameter(0, previewId).setParameter(1, modelId).executeUpdate();
         session.getTransaction().commit();
      } catch (Exception var10) {
         this.logger.error("删除模型片段异常", var10);
      } finally {
         session.close();
      }
   }

   public void deleteModelFragment(long modelId, long previewId) {
      Session session = null;

      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         session.beginTransaction();
         String hql = "DELETE FROM ModelFragment WHERE modelId=? OR previewId=?";
         session.createQuery(hql).setParameter(0, modelId).setParameter(1, previewId).executeUpdate();
         session.getTransaction().commit();
      } catch (Exception var10) {
         this.logger.error("删除模型片段异常", var10);
      } finally {
         session.close();
      }
   }

   public void updateModelFragment(long modelId, long newModelId) {
      Session session = null;

      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         session.beginTransaction();
         String sql = "update model_detail SET model_id=" + newModelId + " where model_id=" + modelId;
         session.createSQLQuery(sql).executeUpdate();
         session.getTransaction().commit();
      } catch (Exception var10) {
         this.logger.error("更新模型片段异常", var10);
      } finally {
         session.close();
      }
   }

   public List<ModelFragment> queryAllFragment(long modelId) throws Exception {
      return (List)this.hiberTemp.findByNamedQuery("queryAllFragment", new Object[]{modelId});
   }

   public void save(ModelFragment modelFragment) {
      this.hiberTemp.save(modelFragment);
   }

   public HibernateTemplate getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplate hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
