/**
 * Copyright (c) 2008, SnakeYAML
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.yaml.snakeyaml.issues.issue1120;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import org.junit.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MergeConflictException;
import org.yaml.snakeyaml.nodes.Node;

/**
 * Strict merge mode must reject a conflict between an explicitly defined key and a key coming from
 * a merge key, reporting the marks of both locations.
 */
public class StrictMergeTest {

  private static final String SOURCE = "defaults: &defaults\n" + "  admin: true\n"
      + "  name: root\n" + "user:\n" + "  <<: *defaults\n" + "  admin: false\n";

  private LoaderOptions strictOptions() {
    LoaderOptions options = new LoaderOptions();
    options.setStrictMerge(true);
    return options;
  }

  @Test
  public void rejectConflictDuringConstruction() {
    Yaml yaml = new Yaml(strictOptions());
    try {
      yaml.load(SOURCE);
      fail("MergeConflictException was expected");
    } catch (MergeConflictException e) {
      assertConflictReported(e);
    }
  }

  @Test
  public void rejectConflictDuringCompose() {
    LoaderOptions options = strictOptions();
    options.setMergeOnCompose(true);
    Yaml yaml = new Yaml(options);
    try {
      yaml.compose(new StringReader(SOURCE));
      fail("MergeConflictException was expected");
    } catch (MergeConflictException e) {
      assertConflictReported(e);
    }
  }

  @Test
  public void rejectConflictWhenMergeComesFirstDuringConstruction() {
    String source = "defaults: &defaults\n" + "  admin: true\n" + "user:\n" + "  admin: false\n"
        + "  <<: *defaults\n";
    Yaml yaml = new Yaml(strictOptions());
    try {
      yaml.load(source);
      fail("MergeConflictException was expected");
    } catch (MergeConflictException e) {
      assertEquals(3, e.getContextMark().getLine());
      assertEquals(1, e.getProblemMark().getLine());
      assertTrue(e.getMessage(), e.getMessage().contains("explicit key admin"));
    }
  }

  @Test
  public void rejectConflictInMergedSequence() {
    String source = "first: &first\n" + "  admin: true\n" + "second: &second\n" + "  role: user\n"
        + "user:\n" + "  <<: [*first, *second]\n" + "  admin: false\n";
    Yaml yaml = new Yaml(strictOptions());
    try {
      yaml.load(source);
      fail("MergeConflictException was expected");
    } catch (MergeConflictException e) {
      Mark explicitMark = e.getContextMark();
      Mark mergedMark = e.getProblemMark();
      assertNotNull(explicitMark);
      assertNotNull(mergedMark);
      assertEquals(6, explicitMark.getLine());
      assertEquals(1, mergedMark.getLine());
      assertTrue(e.getMessage(), e.getMessage().contains("explicit key admin"));
    }
  }

  @Test
  public void rejectConflictFromNestedMerge() {
    String source = "base: &base\n" + "  admin: true\n" + "middle: &middle\n" + "  <<: *base\n"
        + "  name: root\n" + "user:\n" + "  <<: *middle\n" + "  admin: false\n";
    Yaml yaml = new Yaml(strictOptions());
    try {
      yaml.load(source);
      fail("MergeConflictException was expected");
    } catch (MergeConflictException e) {
      assertEquals(7, e.getContextMark().getLine());
      assertEquals(1, e.getProblemMark().getLine());
      assertTrue(e.getMessage(), e.getMessage().contains("explicit key admin"));
    }
  }

  @Test
  public void allowMappingWithoutConflicts() {
    String source = "defaults: &defaults\n" + "  admin: true\n" + "  name: root\n" + "user:\n"
        + "  <<: *defaults\n" + "  enabled: false\n";
    Map<String, Object> result = new Yaml(strictOptions()).load(source);
    Map<?, ?> user = (Map<?, ?>) result.get("user");
    assertEquals(Boolean.TRUE, user.get("admin"));
    assertEquals("root", user.get("name"));
    assertEquals(Boolean.FALSE, user.get("enabled"));
  }

  @Test
  public void keepYaml11PrecedenceWhenDisabled() {
    LoaderOptions options = new LoaderOptions();
    options.setStrictMerge(false);
    Map<String, Object> result = new Yaml(options).load(SOURCE);
    Map<?, ?> user = (Map<?, ?>) result.get("user");
    assertEquals(Boolean.FALSE, user.get("admin"));
    assertEquals("root", user.get("name"));
  }

  @Test
  public void strictMergeIsDisabledByDefault() {
    LoaderOptions options = new LoaderOptions();
    assertEquals(false, options.isStrictMerge());
    options.setStrictMerge(true);
    assertTrue(options.isStrictMerge());
  }

  @Test
  public void strictModeDoesNotChangeSerializationWhenMergeKeptOnNode() {
    LoaderOptions options = strictOptions();
    DumperOptions dumperOptions = new DumperOptions();
    dumperOptions.setDereferenceAliases(true);
    Yaml yaml = new Yaml(options, dumperOptions);
    Node node = yaml.compose(new StringReader(SOURCE));
    StringWriter writer = new StringWriter();
    yaml.serialize(node, writer);
    String output = writer.toString();
    assertTrue(output, output.contains("admin: false"));
  }

  private void assertConflictReported(MergeConflictException e) {
    Mark explicitMark = e.getContextMark();
    Mark mergedMark = e.getProblemMark();
    assertNotNull(explicitMark);
    assertNotNull(mergedMark);
    // explicit "admin: false" is on line 6, merged "admin: true" is on line 2
    assertEquals(5, explicitMark.getLine());
    assertEquals(1, mergedMark.getLine());
    assertTrue(e.getMessage(), e.getMessage().contains("explicit key admin"));
    assertTrue(e.getMessage(), e.getMessage().contains("merged mapping"));
  }
}
