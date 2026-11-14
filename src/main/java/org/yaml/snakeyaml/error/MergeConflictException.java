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
package org.yaml.snakeyaml.error;

/**
 * Indicate that an explicitly defined key conflicts with a key introduced by a merge key
 * (https://yaml.org/type/merge.html).
 * <p>
 * It is thrown only when the strict merge mode is enabled in
 * {@link org.yaml.snakeyaml.LoaderOptions}. The marks point at both the explicit key and the
 * conflicting key in the merged mapping, so that the source of the conflict can be located.
 * </p>
 */
public class MergeConflictException extends MarkedYAMLException {

  private static final long serialVersionUID = 1L;

  /**
   * Create
   *
   * @param key - the key which is both defined explicitly and present in a merged mapping
   * @param explicitMark - position of the explicitly defined key
   * @param mergedMark - position of the conflicting key in the merged mapping
   */
  public MergeConflictException(Object key, Mark explicitMark, Mark mergedMark) {
    super("while processing a merge", explicitMark,
        "explicit key " + key + " conflicts with the same key in a merged mapping", mergedMark);
  }
}
