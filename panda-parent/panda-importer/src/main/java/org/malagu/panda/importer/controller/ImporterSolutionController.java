package org.malagu.panda.importer.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Type;
import org.malagu.panda.dorado.linq.JpaUtil;
import org.malagu.panda.dorado.linq.policy.SaveContext;
import org.malagu.panda.dorado.linq.policy.impl.SmartSavePolicy;
import org.malagu.panda.importer.filter.EntityManagerFactoryFilter;
import org.malagu.panda.importer.filter.EntityTypeFilter;
import org.malagu.panda.importer.model.ImporterSolution;
import org.malagu.panda.importer.model.MappingRule;
import org.malagu.panda.importer.parser.CellPostParser;
import org.malagu.panda.importer.parser.CellPreParser;
import org.malagu.panda.importer.policy.AutoCreateMappingRulePolicy;
import org.malagu.panda.importer.policy.PostProcessPolicy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.annotation.Transactional;
import com.bstek.dorado.annotation.DataProvider;
import com.bstek.dorado.annotation.DataResolver;
import com.bstek.dorado.annotation.Expose;
import com.bstek.dorado.data.provider.Criteria;
import com.bstek.dorado.data.provider.Page;

/**
 * @author Kevin.yang
 * @since 2015年8月23日
 */
@Transactional(readOnly = true)
public class ImporterSolutionController implements ApplicationContextAware {

  private Collection<String> entityManagerFactoryNames;
  private Map<String, List<String>> entityClassNameMap = new HashMap<>();
  private Collection<Map<String, String>> cellPreParsers = new ArrayList<>();
  private Collection<Map<String, String>> cellPostParsers = new ArrayList<>();
  private AutoCreateMappingRulePolicy autoCreateMappingRulePolicy;
  @Autowired
  private Collection<PostProcessPolicy> postProcessPolicyList;

  @DataProvider
  public Collection<PostProcessPolicy> loadPostProcessPolicyList() {
    return new ArrayList<>(postProcessPolicyList);
  }


  @DataProvider
  public void loadImporterSolutions(Page<ImporterSolution> page, Criteria criteria) {
    JpaUtil.linq(ImporterSolution.class)
        .where(criteria)
        .paging(page);
  }

  @DataProvider
  public void loadMappingRules(Page<MappingRule> page, Criteria criteria,
      String importerSolutionId) {
    JpaUtil.linq(MappingRule.class)
        .where(criteria)
        .equal("importerSolutionId", importerSolutionId)
        .asc("excelColumn")
        .paging(page);
  }

  @DataProvider
  public Collection<String> loadEntityManagerFactoryNames() {
    return entityManagerFactoryNames;
  }

  @DataProvider
  public Collection<String> loadEntityClassNames(String sessionFactoryName) {
    return entityClassNameMap.get(sessionFactoryName);
  }

  @DataProvider
  public Collection<Map<String, String>> loadCellPreParsers() {
    return cellPreParsers;
  }

  @DataProvider
  public Collection<Map<String, String>> loadCellPostParsers() {
    return cellPostParsers;
  }

  @DataResolver
  @Transactional
  public void saveImporterSolutions(Collection<ImporterSolution> importerSolutions) {
    JpaUtil.save(importerSolutions, new SmartSavePolicy() {

      @Override
      public void apply(SaveContext context) {
        if (context.getEntity() instanceof org.malagu.panda.importer.model.Entry) {
          MappingRule mappingRule = context.getParent();
          org.malagu.panda.importer.model.Entry entry = context.getEntity();
          entry.setMappingRuleId(mappingRule.getId());
        }
        super.apply(context);
      }

    });
  }

  @Expose
  @Transactional
  public void autoCreateMappingRules(ImporterSolution importerSolution) {
    autoCreateMappingRulePolicy.apply(importerSolution);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext)
      throws BeansException {
    Map<String, EntityManagerFactory> entityManagerFactoryMap =
        new HashMap<String, EntityManagerFactory>(
            applicationContext.getBeansOfType(EntityManagerFactory.class));

    Collection<EntityManagerFactoryFilter> sessionFactoryFilters = applicationContext
        .getBeansOfType(EntityManagerFactoryFilter.class).values();

    for (EntityManagerFactoryFilter sessionFactoryFilter : sessionFactoryFilters) {
      sessionFactoryFilter.filter(entityManagerFactoryMap);
    }

    Collection<EntityTypeFilter> entityTypeFilters = applicationContext
        .getBeansOfType(EntityTypeFilter.class).values();

    for (Entry<String, EntityManagerFactory> entry : entityManagerFactoryMap.entrySet()) {
      List<EntityType<?>> entityTypes =
          new ArrayList<EntityType<?>>(entry.getValue().getMetamodel().getEntities());
      for (EntityTypeFilter entityTypeFilter : entityTypeFilters) {
        entityTypeFilter.filter(entityTypes);
      }
      List<String> entityClassNames = new ArrayList<String>();
      for (EntityType<?> entityType : entityTypes) {
        if (entityType instanceof Type) {
          entityClassNames.add(entityType.getJavaType().getName());
        }
      }
      entityClassNameMap.put(entry.getKey(), entityClassNames);
    }
    entityManagerFactoryNames = new ArrayList<String>(entityManagerFactoryMap.keySet());

    Map<String, CellPreParser> cellPreParserMap =
        applicationContext.getBeansOfType(CellPreParser.class);
    for (Entry<String, CellPreParser> entry : cellPreParserMap.entrySet()) {
      Map<String, String> map = new HashMap<>();
      map.put("beanId", entry.getKey());
      map.put("parserName", entry.getValue().getName());
      cellPreParsers.add(map);
    }

    Map<String, CellPostParser> cellPostParserMap =
        applicationContext.getBeansOfType(CellPostParser.class);
    for (Entry<String, CellPostParser> entry : cellPostParserMap.entrySet()) {
      Map<String, String> map = new HashMap<>();
      map.put("beanId", entry.getKey());
      map.put("parserName", entry.getValue().getName());
      cellPostParsers.add(map);
    }

  }

  public void setAutoCreateMappingRulePolicy(
      AutoCreateMappingRulePolicy autoCreateMappingRulePolicy) {
    this.autoCreateMappingRulePolicy = autoCreateMappingRulePolicy;
  }

}
