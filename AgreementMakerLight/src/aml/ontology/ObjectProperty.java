/******************************************************************************
* Copyright 2013-2015 LASIGE                                                  *
*                                                                             *
* Licensed under the Apache License, Version 2.0 (the "License"); you may     *
* not use this file except in compliance with the License. You may obtain a   *
* copy of the License at http://www.apache.org/licenses/LICENSE-2.0           *
*                                                                             *
* Unless required by applicable law or agreed to in writing, software         *
* distributed under the License is distributed on an "AS IS" BASIS,           *
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    *
* See the License for the specific language governing permissions and         *
* limitations under the License.                                              *
*                                                                             *
*******************************************************************************
* An Ontology Object Property Entity.                                         *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 21-05-2015                                                            *
******************************************************************************/
package aml.ontology;

import java.util.HashSet;
import java.util.Set;

public class ObjectProperty extends Entity
{
	
//Attributes
	
	private Set<Integer> domain;
	private Set<Integer> range;
	private boolean isFunctional;
	
//Constructors

	public ObjectProperty(String n)
	{
		super(n);
		domain = new HashSet<Integer>();
		range = new HashSet<Integer>();
		isFunctional = false;
	}
	
	public ObjectProperty(String n, boolean f)
	{
		this(n);
		isFunctional = f;
	}
	
//Public Methods

	public void addDomain(Integer i)
	{
		domain.add(i);
	}
	
	public void addRange(Integer i)
	{
		range.add(i);
	}
	
	public Set<Integer> getDomain()
	{
		return domain;
	}
		
	public Set<Integer> getRange()
	{
		return range;
	}
	
	public boolean isFunctional()
	{
		return isFunctional;
	}
	
	public void isFunctional(boolean f)
	{
		isFunctional = f;
	}
}