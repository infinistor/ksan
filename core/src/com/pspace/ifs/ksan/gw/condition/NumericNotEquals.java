/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.ifs.ksan.gw.condition;

import java.util.ArrayList;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.format.Tagging;
import com.pspace.ifs.ksan.gw.format.Tagging.TagSet.Tag;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.libs.PrintStack;

import org.slf4j.LoggerFactory;

public class NumericNotEquals extends PolicyCondition {
    public NumericNotEquals(JsonNode jsonNode) {
        super(jsonNode);
        logger = LoggerFactory.getLogger(NumericNotEquals.class);
    }
    
    @Override
    public void process() throws GWException {
        this.value = new ArrayList<String>();
        if (jsonNode.isObject()) {
            Iterator<String> fieldNames = jsonNode.fieldNames();
            if (fieldNames.hasNext()) {
                // read key
                String fieldName = fieldNames.next();
                this.key = fieldName;
                logger.info(GWConstants.LOG_KEY, this.key);

                // read value
                JsonNode fieldValue = jsonNode.get(fieldName);
                if (fieldValue != null && fieldValue.isArray() ) {
                    ArrayNode arrayNode = (ArrayNode) fieldValue;
                    for (int i=0; i<arrayNode.size(); i++) {
                        JsonNode arrayElement = arrayNode.get(i);
                        logger.info(GWConstants.LOG_VALUE, arrayElement.textValue());
                        value.add(arrayElement.textValue());
                    }
                } else if (fieldValue != null && fieldValue.isTextual()) {
                    logger.info(GWConstants.LOG_VALUE, fieldValue.textValue());
                    value.add(fieldValue.textValue());
                }
            }
        }
    }

    @Override
    public boolean compare(String comp) throws GWException {
        boolean ret = false;

        if (Strings.isNullOrEmpty(comp)) {
            return ret;
        }
        
        for (String s : value) {
            if (Double.parseDouble(s) != Double.parseDouble(comp)) {
                ret = true;
                break;
            }
        }

        return ret;
    }

    @Override
    public boolean compareTagging(S3Parameter s3Parameter) throws GWException {
        boolean conditioncheck = false;
        String tagkey = key.replace(GWConstants.KEY_EXISTING_OBJECT_TAG + GWConstants.SLASH, GWConstants.EMPTY_STRING);

        String tagvalue = null;
        for (String v : value) {
            logger.info(v);
            tagvalue = v;
            break;
        }
        
        if (!Strings.isNullOrEmpty(s3Parameter.getTaggingInfo()) ) {
            try {
                Tagging tagging = new XmlMapper().readValue(s3Parameter.getTaggingInfo(), Tagging.class);

                if (tagging != null) {
                    if (tagging.tagset != null && tagging.tagset.tags != null) {
                        for (Tag t : tagging.tagset.tags) {
                            logger.info(GWConstants.LOG_T_KEY, tagkey);
                            logger.info(GWConstants.LOG_T_VALUE, tagvalue);
                            if (t.key.equals(tagkey) && t.value.equals(tagvalue)) {
                                conditioncheck = true;
                                break;
                            }
                        }
                    }
                }
            } catch (JsonMappingException e) {
                PrintStack.logging(logger, e);
                throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
            } catch (JsonProcessingException e) {
                PrintStack.logging(logger, e);
                throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
            }
        } else {
            conditioncheck = true;
        }

        return conditioncheck;
    }
}
