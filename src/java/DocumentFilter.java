/*
 * Copyright 2010 Internet Archive
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import org.archive.hadoop.DocumentProperties;

/**
 * Interface for family of implementors which filter out documents
 * based on arbitrary rules.
 *
 * The DocumentProperties are given to the filter, which returns true
 * if the document is allowed and false if not.
 */
public interface DocumentFilter
{
  public boolean isAllowed( DocumentProperties properties );
}
