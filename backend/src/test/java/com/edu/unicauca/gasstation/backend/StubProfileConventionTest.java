package com.edu.unicauca.gasstation.backend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

/**
 * Guard for the stub convention: every class inside a "stub" package must be limited to
 * the Spring profile "stub". Without it, a forgotten annotation would send fake data to production.
 * No setup is needed when a new stub is added: it only has to follow the convention.
 *
 * @author javiersolanop777
 */
class StubProfileConventionTest {

    private static final String BASE_PACKAGE = "com.edu.unicauca.gasstation.backend";
    private static final String STUB_PROFILE = "stub";

    @Test
    void everyClassInAStubPackageIsOnlyActiveWithTheStubProfile() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider objScanner = new ClassPathScanningCandidateComponentProvider(false);
        // Classes inside a ".stub." package, except test classes (their names end with "Test")
        objScanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile("^(?!.*Test$).*\\.stub\\..*$")));

        List<String> objInvalid = new ArrayList<>();
        for (BeanDefinition objDefinition : objScanner.findCandidateComponents(BASE_PACKAGE)) {
            Class<?> objType = Class.forName(objDefinition.getBeanClassName());
            Profile objProfile = AnnotatedElementUtils.findMergedAnnotation(objType, Profile.class);
            if (objProfile == null || !Arrays.asList(objProfile.value()).contains(STUB_PROFILE)) {
                objInvalid.add(objType.getName());
            }
        }
        assertTrue(objInvalid.isEmpty(),
                "These classes are in a stub package but do not use @Profile(\"" + STUB_PROFILE + "\"): " + objInvalid);
    }
}
