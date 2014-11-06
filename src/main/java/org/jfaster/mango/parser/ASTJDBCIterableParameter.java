/*
 * Copyright 2014 mango.jfaster.org
 *
 * The Mango Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.jfaster.mango.parser;

import org.jfaster.mango.exception.UnreachableCodeException;
import org.jfaster.mango.operator.InvocationContext;
import org.jfaster.mango.util.Iterables;
import org.jfaster.mango.util.Strings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 可迭代参数
 *
 * @author ash
 */
public class ASTJDBCIterableParameter extends AbstractRenderableNode implements ParameterBean{

    private String parameterName;
    private String propertyPath; // 为""的时候表示没有属性

    private String interableProperty; // "a in (:1)"中的a

    public ASTJDBCIterableParameter(int i) {
        super(i);
    }

    public ASTJDBCIterableParameter(Parser p, int i) {
        super(p, i);
    }

    public void init(String str) {
        Pattern p = Pattern.compile("in\\s*\\(\\s*(:(\\w+)(\\.\\w+)*)\\s*\\)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(str);
        if (!m.matches()) {
            throw new UnreachableCodeException();
        }
        String fullName = m.group(1);
        parameterName = m.group(2);
        propertyPath = fullName.substring(parameterName.length() + 1);
        if (!propertyPath.isEmpty()) {
            propertyPath = propertyPath.substring(1);  // .a.b.c变为a.b.c
        }
    }

    @Override
    public boolean render(InvocationContext context) {
        Object objs = context.getNullablePropertyValue(parameterName, propertyPath);
        if (objs == null) {
            throw new NullPointerException("value of " +
                    Strings.getFullName(parameterName, propertyPath) + " can't be null");
        }
        Iterables iterables = new Iterables(objs);
        if (iterables.isEmpty()) {
            throw new IllegalArgumentException("value of " +
                    Strings.getFullName(parameterName, propertyPath) + " can't be empty");
        }
        context.writeToSqlBuffer("in (");
        int t = 0;
        for (Object obj : iterables) {
            context.appendToArgs(obj);
            if (t == 0) {
                context.writeToSqlBuffer("?");
            } else {
                context.writeToSqlBuffer(",?");
            }
            t++;
        }
        context.writeToSqlBuffer(")");
        return true;
    }

    @Override
    public String toString() {
        return super.toString() + "{" +
                "fullName=" + getFullName() + ", " +
                "parameterName=" + parameterName + ", " +
                "propertyPath=" + propertyPath +
                "}";
    }

    @Override
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    @Override
    public boolean onlyParameterName() {
        return Strings.isEmpty(propertyPath);
    }

    @Override
    public String getParameterName() {
        return parameterName;
    }

    @Override
    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    @Override
    public String getPropertyPath() {
        return propertyPath;
    }

    @Override
    public void setPropertyPath(String propertyPath) {
        this.propertyPath = propertyPath;
    }

    @Override
    public String getFullName() {
        return Strings.getFullName(parameterName, propertyPath);
    }

    public void setInterableProperty(String interableProperty) {
        this.interableProperty = interableProperty;
    }

    public String getInterableProperty() {
        return interableProperty;
    }

}