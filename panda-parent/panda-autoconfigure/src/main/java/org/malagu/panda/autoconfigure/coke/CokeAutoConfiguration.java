package org.malagu.panda.autoconfigure.coke;

import org.malagu.panda.autoconfigure.security.SecurityAutoConfiguration;
import org.malagu.panda.coke.CokeConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Kevin Yang (mailto:kevin.yang@bstek.com)
 * @since 2017年2月7日
 */
@Configuration
@ConditionalOnClass(CokeConfiguration.class)
@AutoConfigureBefore({ JpaRepositoriesAutoConfiguration.class })
@AutoConfigureAfter(SecurityAutoConfiguration.class)
@Import(CokeConfiguration.class)
public class CokeAutoConfiguration {

}
