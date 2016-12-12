/*
 * Copyright 1999-2101 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.druid.sql.dialect.oracle.parser;

import com.alibaba.druid.sql.ast.SQLDataType;
import com.alibaba.druid.sql.ast.SQLDataTypeImpl;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLOrderingSpecification;
import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;
import com.alibaba.druid.sql.ast.expr.SQLAggregateOption;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLNumberExpr;
import com.alibaba.druid.sql.ast.expr.SQLNumericLiteralExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.expr.SQLTimestampExpr;
import com.alibaba.druid.sql.ast.expr.SQLUnaryExpr;
import com.alibaba.druid.sql.ast.expr.SQLUnaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLCharacterDataType;
import com.alibaba.druid.sql.ast.statement.SQLCheck;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLForeignKeyConstraint;
import com.alibaba.druid.sql.ast.statement.SQLUnique;
import com.alibaba.druid.sql.dialect.oracle.ast.OracleDataTypeIntervalDay;
import com.alibaba.druid.sql.dialect.oracle.ast.OracleDataTypeIntervalYear;
import com.alibaba.druid.sql.dialect.oracle.ast.OracleDataTypeTimestamp;
import com.alibaba.druid.sql.dialect.oracle.ast.OracleOrderBy;
import com.alibaba.druid.sql.dialect.oracle.ast.clause.OracleLobStorageClause;
import com.alibaba.druid.sql.dialect.oracle.ast.clause.OracleStorageClause;
import com.alibaba.druid.sql.dialect.oracle.ast.clause.OracleStorageClause.FlashCacheType;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleAnalytic;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleAnalyticWindowing;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleArgumentExpr;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleBinaryDoubleExpr;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleBinaryFloatExpr;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleCursorExpr;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleDateExpr;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleDateTimeUnit;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleDatetimeExpr;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleDbLinkExpr;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleExtractExpr;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleIntervalExpr;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleIntervalType;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleIsSetExpr;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleOuterExpr;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleRangeExpr;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleSizeExpr;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleSysdateExpr;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleCheck;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleConstraint;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleConstraint.Initially;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleForeignKey;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleOrderByItem;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OraclePrimaryKey;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleSelect;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleUnique;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleUsingIndexClause;
import com.alibaba.druid.sql.lexer.Lexer;
import com.alibaba.druid.sql.lexer.Token;
import com.alibaba.druid.sql.parser.ParserException;
import com.alibaba.druid.sql.parser.ParserUnsupportedException;
import com.alibaba.druid.sql.parser.SQLExprParser;
import com.alibaba.druid.util.JdbcConstants;

import java.math.BigInteger;

public class OracleExprParser extends SQLExprParser {
    
    private static final String[] AGGREGATE_FUNCTIONS = {"MAX", "MIN", "COUNT", "SUM", "AVG", "CORR", "COVAR_POP", "COVAR_SAMP", "CUME_DIST", "DENSE_RANK", 
            "FIRST", "FIRST_VALUE", "LAG", "LAST", "LAST_VALUE", "LISTAGG", "LEAD", "NTILE", "PERCENT_RANK", "PERCENTILE_CONT", "PERCENTILE_DISC", "RANK", "RATIO_TO_REPORT", 
            "REGR_SLOPE", "REGR_INTERCEPT", "REGR_INTERCEPT", "REGR_COUNT", "REGR_R2", "REGR_AVGX", "REGR_AVGY", "REGR_SXX", "REGR_SYY", "REGR_SXY", 
            "ROW_NUMBER", "STDDEV", "STDDEV_POP", "STDDEV_SAMP", "VAR_POP", "VAR_SAMP", "VARIANCE", "WM_CONCAT"};
    
    public OracleExprParser(final String sql){
        super(new OracleLexer(sql), JdbcConstants.ORACLE, AGGREGATE_FUNCTIONS);
        getLexer().nextToken();
    }
    
    public OracleExprParser(Lexer lexer){
        super(lexer, JdbcConstants.ORACLE, AGGREGATE_FUNCTIONS);
    }
    
    protected boolean isCharType(String dataTypeName) {
        return "varchar2".equalsIgnoreCase(dataTypeName) || "nvarchar2".equalsIgnoreCase(dataTypeName)
                || "char".equalsIgnoreCase(dataTypeName) || "varchar".equalsIgnoreCase(dataTypeName) 
                || "nchar".equalsIgnoreCase(dataTypeName) || "nvarchar".equalsIgnoreCase(dataTypeName);
    }
    
    public SQLDataType parseDataType() {
        if (getLexer().equalToken(Token.CONSTRAINT) || getLexer().equalToken(Token.COMMA)) {
            return null;
        }
        
        if (getLexer().equalToken(Token.DEFAULT) || getLexer().equalToken(Token.NOT) || getLexer().equalToken(Token.NULL)) {
            return null;
        }
        
        if (getLexer().equalToken(Token.INTERVAL)) {
            getLexer().nextToken();
            if (getLexer().identifierEquals("YEAR")) {
                getLexer().nextToken();
                OracleDataTypeIntervalYear interval = new OracleDataTypeIntervalYear();
                
                if (getLexer().equalToken(Token.LEFT_PAREN)) {
                    getLexer().nextToken();
                    interval.getArguments().add(this.expr());
                    accept(Token.RIGHT_PAREN);
                }
                
                accept(Token.TO);
                acceptIdentifier("MONTH");
                
                return interval;
            } else {
                acceptIdentifier("DAY");
                OracleDataTypeIntervalDay interval = new OracleDataTypeIntervalDay();
                if (getLexer().equalToken(Token.LEFT_PAREN)) {
                    getLexer().nextToken();
                    interval.getArguments().add(this.expr());
                    accept(Token.RIGHT_PAREN);
                }
                
                accept(Token.TO);
                acceptIdentifier("SECOND");
                
                if (getLexer().equalToken(Token.LEFT_PAREN)) {
                    getLexer().nextToken();
                    interval.getFractionalSeconds().add(this.expr());
                    accept(Token.RIGHT_PAREN);
                }
                
                return interval;
            }
        }
        
        String typeName;
        if (getLexer().identifierEquals("LONG")) {
            getLexer().nextToken();
            acceptIdentifier("RAW");
            typeName = "LONG RAW";
        } else {
            SQLName typeExpr = name();
            typeName = typeExpr.toString();
        }
        
        if ("TIMESTAMP".equalsIgnoreCase(typeName)) {
            OracleDataTypeTimestamp timestamp = new OracleDataTypeTimestamp();
            
            if (getLexer().equalToken(Token.LEFT_PAREN)) {
                getLexer().nextToken();
                timestamp.getArguments().add(this.expr());
                accept(Token.RIGHT_PAREN);
            }
            
            if (getLexer().equalToken(Token.WITH)) {
                getLexer().nextToken();
                
                if (getLexer().identifierEquals("LOCAL")) {
                    getLexer().nextToken();
                    timestamp.setWithLocalTimeZone(true);
                } else {
                    timestamp.setWithTimeZone(true);
                }
                
                acceptIdentifier("TIME");
                acceptIdentifier("ZONE");
            }
            
            return timestamp;
        }
        
        if (isCharType(typeName)) {
            SQLCharacterDataType charType = new SQLCharacterDataType(typeName);
            
            if (getLexer().equalToken(Token.LEFT_PAREN)) {
                getLexer().nextToken();
                
                charType.getArguments().add(this.expr());
                
                if (getLexer().identifierEquals("CHAR")) {
                    getLexer().nextToken();
                    charType.setCharType(SQLCharacterDataType.CHAR_TYPE_CHAR);
                } else if (getLexer().identifierEquals("BYTE")) {
                    getLexer().nextToken();
                    charType.setCharType(SQLCharacterDataType.CHAR_TYPE_BYTE);
                }
                
                accept(Token.RIGHT_PAREN);
            }
            
            return parseCharTypeRest(charType);
        }
        
        if (getLexer().equalToken(Token.PERCENT)) {
            getLexer().nextToken();
            if (getLexer().identifierEquals("TYPE")) {
                getLexer().nextToken();
                typeName += "%TYPE";
            } else if (getLexer().identifierEquals("ROWTYPE")) {
                getLexer().nextToken();
                typeName += "%ROWTYPE";
            } else {
                throw new ParserException(getLexer());
            }
        }
        SQLDataType dataType = new SQLDataTypeImpl(typeName);
        return parseDataTypeRest(dataType);
    }
    
    public SQLExpr primary() {
        final Token tok = getLexer().getToken();
        SQLExpr sqlExpr;
        switch (tok) {
            case SYSDATE:
                getLexer().nextToken();
                OracleSysdateExpr sysdate = new OracleSysdateExpr();
                if (getLexer().equalToken(Token.MONKEYS_AT)) {
                    getLexer().nextToken();
                    accept(Token.BANG);
                    sysdate.setOption("!");
                }
                sqlExpr = sysdate;
                return primaryRest(sqlExpr);
            case PRIOR:
                getLexer().nextToken();
                sqlExpr = expr();
                sqlExpr = new SQLUnaryExpr(SQLUnaryOperator.Prior, sqlExpr);
                return primaryRest(sqlExpr);
            case COLON:
                getLexer().nextToken();
                if (getLexer().equalToken(Token.LITERAL_INT)) {
                    String name = ":" + getLexer().getTerm().getValue();
                    getLexer().nextToken();
                    return new SQLVariantRefExpr(name);
                } else if (getLexer().equalToken(Token.IDENTIFIER)) {
                    String name = getLexer().getLiterals();
                    if (name.charAt(0) == 'B' || name.charAt(0) == 'b') {
                        getLexer().nextToken();
                        return new SQLVariantRefExpr(":" + name);
                    }
                    throw new ParserException(getLexer());
                } else {
                    throw new ParserException(getLexer());
                }
            case LITERAL_ALIAS:
                String alias = '"' + getLexer().getLiterals() + '"';
                getLexer().nextToken();
                return primaryRest(new SQLIdentifierExpr(alias));
            case EXTRACT:
                getLexer().nextToken();
                OracleExtractExpr extract = new OracleExtractExpr();

                accept(Token.LEFT_PAREN);

                extract.setUnit(OracleDateTimeUnit.valueOf(getLexer().getLiterals().toUpperCase()));
                getLexer().nextToken();

                accept(Token.FROM);

                extract.setFrom(expr());

                accept(Token.RIGHT_PAREN);

                return primaryRest(extract);
            case BINARY_FLOAT:
                OracleBinaryFloatExpr floatExpr = new OracleBinaryFloatExpr();
                floatExpr.setValue(Float.parseFloat(getLexer().getTerm().getValue()));
                getLexer().nextToken();
                return primaryRest(floatExpr);
            case BINARY_DOUBLE:
                OracleBinaryDoubleExpr doubleExpr = new OracleBinaryDoubleExpr();
                doubleExpr.setValue(Double.parseDouble(getLexer().getTerm().getValue()));

                getLexer().nextToken();
                return primaryRest(doubleExpr);
            case TABLE:
                getLexer().nextToken();
                return primaryRest(new SQLIdentifierExpr("TABLE"));
            case PLUS:
                getLexer().nextToken();
                switch (getLexer().getToken()) {
                    case LITERAL_INT:
                        sqlExpr = new SQLIntegerExpr(getLexer().integerValue());
                        getLexer().nextToken();
                        break;
                    case LITERAL_FLOAT:
                        sqlExpr = new SQLNumberExpr(getLexer().decimalValue());
                        getLexer().nextToken();
                        break;
                    case BINARY_FLOAT:
                        sqlExpr = new OracleBinaryFloatExpr(Float.parseFloat(getLexer().getTerm().getValue()));
                        getLexer().nextToken();
                        break;
                    case BINARY_DOUBLE:
                        sqlExpr = new OracleBinaryDoubleExpr(Double.parseDouble(getLexer().getTerm().getValue()));
                        getLexer().nextToken();
                        break;
                    case LEFT_PAREN:
                        getLexer().nextToken();
                        sqlExpr = expr();
                        accept(Token.RIGHT_PAREN);
                        sqlExpr = new SQLUnaryExpr(SQLUnaryOperator.Plus, sqlExpr);
                        break;
                    default:
                        throw new ParserUnsupportedException(getLexer().getToken());
                }
                return primaryRest(sqlExpr);
            case SUB:
                getLexer().nextToken();
                switch (getLexer().getToken()) {
                    case LITERAL_INT:
                        Number integerValue = getLexer().integerValue();
                        if (integerValue instanceof Integer) {
                            int intVal = (Integer) integerValue;
                            if (intVal == Integer.MIN_VALUE) {
                                integerValue = ((long) intVal) * -1;
                            } else {
                                integerValue = intVal * -1;
                            }
                        } else if (integerValue instanceof Long) {
                            long longVal = integerValue.longValue();
                            if (longVal == 2147483648L) {
                                integerValue = (int) (longVal * -1);
                            } else {
                                integerValue = longVal * -1;
                            }
                        } else {
                            integerValue = ((BigInteger) integerValue).negate();
                        }
                        sqlExpr = new SQLIntegerExpr(integerValue);
                        getLexer().nextToken();
                        break;
                    case LITERAL_FLOAT:
                        sqlExpr = new SQLNumberExpr(getLexer().decimalValue().negate());
                        getLexer().nextToken();
                        break;
                    case BINARY_FLOAT:
                        sqlExpr = new OracleBinaryFloatExpr(Float.parseFloat(getLexer().getTerm().getValue()) * -1);
                        getLexer().nextToken();
                        break;
                    case BINARY_DOUBLE:
                        sqlExpr = new OracleBinaryDoubleExpr(Double.parseDouble(getLexer().getTerm().getValue()) * -1);
                        getLexer().nextToken();
                        break;
                    case VARIANT:
                    case IDENTIFIER:
                        sqlExpr = expr();
                        sqlExpr = new SQLUnaryExpr(SQLUnaryOperator.Negative, sqlExpr);
                        break;
                    case LEFT_PAREN:
                        getLexer().nextToken();
                        sqlExpr = expr();
                        accept(Token.RIGHT_PAREN);
                        sqlExpr = new SQLUnaryExpr(SQLUnaryOperator.Negative, sqlExpr);
                        break;
                    default:
                        throw new ParserUnsupportedException(getLexer().getToken());
                }
                return primaryRest(sqlExpr);
                
           case CURSOR:
                    getLexer().nextToken();
                    accept(Token.LEFT_PAREN);
                    
                    OracleSelect select = createSelectParser().select();
                    OracleCursorExpr cursorExpr = new OracleCursorExpr(select);
                    
                    accept(Token.RIGHT_PAREN);
                    
                    sqlExpr = cursorExpr;
                    return  primaryRest(sqlExpr);
           case MODEL:
           case PCTFREE:
           case INITRANS:
           case MAXTRANS:
           case SEGMENT:
           case CREATION:
           case IMMEDIATE:
           case DEFERRED:
           case STORAGE:
           case NEXT:
           case MINEXTENTS:
           case MAXEXTENTS:
           case MAXSIZE:
           case PCTINCREASE:
           case FLASH_CACHE:
           case CELL_FLASH_CACHE:
           case KEEP:
           case NONE:
           case LOB:
           case STORE:
           case ROW:
           case CHUNK:
           case CACHE:
           case NOCACHE:
           case LOGGING:
           case NOCOMPRESS:
           case KEEP_DUPLICATES:
           case EXCEPTIONS:
           case PURGE:
               sqlExpr = new SQLIdentifierExpr(getLexer().getLiterals());
               getLexer().nextToken();
               return  primaryRest(sqlExpr);
            default:
                return super.primary();
        }
    }
    
    @Override
    protected SQLExpr methodRest(SQLExpr expr, boolean acceptLPAREN) {
        if (acceptLPAREN) {
            accept(Token.LEFT_PAREN);
        }
        
        if (getLexer().equalToken(Token.PLUS)) {
            getLexer().nextToken();
            accept(Token.RIGHT_PAREN);
            return new OracleOuterExpr(expr);
        }
        
        if (expr instanceof SQLIdentifierExpr) {
            String methodName = ((SQLIdentifierExpr) expr).getName();
            SQLMethodInvokeExpr methodExpr = new SQLMethodInvokeExpr(methodName);
            if ("trim".equalsIgnoreCase(methodName)) {
                if (getLexer().identifierEquals("LEADING") //
                        || getLexer().identifierEquals("TRAILING") //
                        || getLexer().identifierEquals("BOTH")
                        ) {
                    methodExpr.putAttribute("trim_option", getLexer().getLiterals());
                    getLexer().nextToken();
                }
                SQLExpr trim_character = this.primary();
                trim_character.setParent(methodExpr);
                methodExpr.putAttribute("trim_character", trim_character);
                if (getLexer().equalToken(Token.FROM)) {
                    getLexer().nextToken();
                    SQLExpr trim_source = this.expr();
                    methodExpr.addParameter(trim_source);
                }
                
                accept(Token.RIGHT_PAREN);
                return primaryRest(methodExpr);
            }
        }
        
        return super.methodRest(expr, false);
    }

    public SQLExpr primaryRest(SQLExpr expr) {
        if (expr.getClass() == SQLIdentifierExpr.class) {
            String ident = ((SQLIdentifierExpr)expr).getName();
            
            if ("DATE".equalsIgnoreCase(ident)) {
                OracleDateExpr timestamp = new OracleDateExpr();

                String literal = getLexer().getLiterals();
                timestamp.setLiteral(literal);
                accept(Token.LITERAL_CHARS);
                
                return primaryRest(timestamp);     
            }
            
            if ("TIMESTAMP".equalsIgnoreCase(ident)) {
                if (!getLexer().equalToken(Token.LITERAL_ALIAS) && !getLexer().equalToken(Token.LITERAL_CHARS)) {
                    return new SQLIdentifierExpr("TIMESTAMP");
                }

                SQLTimestampExpr timestamp = new SQLTimestampExpr();

                String literal = getLexer().getLiterals();
                timestamp.setLiteral(literal);
                accept(Token.LITERAL_CHARS);

                if (getLexer().identifierEquals("AT")) {
                    getLexer().nextToken();
                    acceptIdentifier("TIME");
                    acceptIdentifier("ZONE");

                    String timezone = getLexer().getLiterals();
                    timestamp.setTimeZone(timezone);
                    accept(Token.LITERAL_CHARS);
                }

                
                return primaryRest(timestamp);     
            }
        }
        if (getLexer().equalToken(Token.IDENTIFIER) && expr instanceof SQLNumericLiteralExpr) {
            String ident = getLexer().getLiterals();
            
            if (ident.length() == 1) {
                char unit = ident.charAt(0);
                switch (unit) {
                    case 'K':
                    case 'M':
                    case 'G':
                    case 'T':
                    case 'P':
                    case 'E':
                    case 'k':
                    case 'm':
                    case 'g':
                    case 't':
                    case 'p':
                    case 'e':
                        expr = new OracleSizeExpr(expr, OracleSizeExpr.Unit.valueOf(ident.toUpperCase()));
                        getLexer().nextToken();
                        break;
                    default:
                    break;
                }
            }
        }
        
        if (getLexer().equalToken(Token.DOUBLE_DOT)) {
            getLexer().nextToken();
            SQLExpr upBound = expr();
            
            return new OracleRangeExpr(expr, upBound);
        }
        
        if (getLexer().equalToken(Token.MONKEYS_AT)) {
            getLexer().nextToken();

            OracleDbLinkExpr dblink = new OracleDbLinkExpr();
            dblink.setExpr(expr);

            if (getLexer().equalToken(Token.BANG)) {
                dblink.setDbLink("!");
                getLexer().nextToken();
            } else {
                String link = getLexer().getLiterals();
                accept(Token.IDENTIFIER);
                dblink.setDbLink(link);
            }

            expr = dblink;
        }
        
        if (getLexer().identifierEquals("DAY") || getLexer().identifierEquals("YEAR")) {
            int originalPosition = getLexer().getCurrentPosition();
            String name = getLexer().getLiterals();
            getLexer().nextToken();
            if (getLexer().equalToken(Token.COMMA)) {
                getLexer().setCurrentPosition(originalPosition);
                return expr;
            }
            
            OracleIntervalExpr interval = new OracleIntervalExpr();
            interval.setValue(expr);
            OracleIntervalType type = OracleIntervalType.valueOf(name);
            interval.setType(type);
            
            if (getLexer().equalToken(Token.LEFT_PAREN)) {
                getLexer().nextToken();
                if (!getLexer().equalToken(Token.LITERAL_INT)) {
                    throw new ParserException(getLexer());
                }
                interval.setPrecision(getLexer().integerValue().intValue());
                getLexer().nextToken();
                accept(Token.RIGHT_PAREN);
            }
            
            accept(Token.TO);
            if (getLexer().identifierEquals("SECOND")) {
                getLexer().nextToken();
                interval.setToType(OracleIntervalType.SECOND);
                if (getLexer().equalToken(Token.LEFT_PAREN)) {
                    getLexer().nextToken();
                    if (!getLexer().equalToken(Token.LITERAL_INT)) {
                        throw new ParserException(getLexer());
                    }
                    interval.setFactionalSecondsPrecision(getLexer().integerValue().intValue());
                    getLexer().nextToken();
                    accept(Token.RIGHT_PAREN);
                }
            } else {
                interval.setToType(OracleIntervalType.MONTH);
                getLexer().nextToken();
            }
            
            expr = interval;
        }
        
        if (getLexer().identifierEquals("AT")) {
            int currentPosition = getLexer().getCurrentPosition();
            getLexer().nextToken();
            if (getLexer().identifierEquals("LOCAL")) {
                getLexer().nextToken();
                expr = new OracleDatetimeExpr(expr, new SQLIdentifierExpr("LOCAL"));
            } else {
                if (getLexer().identifierEquals("TIME")) {
                    getLexer().nextToken();
                } else {
                    getLexer().setCurrentPosition(currentPosition);
                    getLexer().setToken(Token.IDENTIFIER);
                    return expr;
                }
                acceptIdentifier("ZONE");
                
                SQLExpr timeZone = primary();
                expr = new OracleDatetimeExpr(expr, timeZone);
            }
        }
        
        SQLExpr restExpr = super.primaryRest(expr);
        
        if (restExpr != expr && restExpr instanceof SQLMethodInvokeExpr) {
            SQLMethodInvokeExpr methodInvoke = (SQLMethodInvokeExpr) restExpr;
            if (methodInvoke.getParameters().size() == 1) {
                SQLExpr paramExpr = methodInvoke.getParameters().get(0);
                if (paramExpr instanceof SQLIdentifierExpr && "+".equals(((SQLIdentifierExpr) paramExpr).getName())) {
                    OracleOuterExpr outerExpr = new OracleOuterExpr();
                    if (methodInvoke.getOwner() == null) {
                        outerExpr.setExpr(new SQLIdentifierExpr(methodInvoke.getMethodName()));
                    } else {
                        outerExpr.setExpr(new SQLPropertyExpr(methodInvoke.getOwner(), methodInvoke.getMethodName()));
                    }
                    return outerExpr;
                }
            }
        }
        
        return restExpr;
    }

    protected SQLExpr dotRest(SQLExpr expr) {
        if (getLexer().equalToken(Token.LITERAL_ALIAS)) {
            String name = '"' + getLexer().getLiterals() + '"';
            getLexer().nextToken();
            expr = new SQLPropertyExpr(expr, name);
            
            if (getLexer().equalToken(Token.DOT)) {
                getLexer().nextToken();
                expr = dotRest(expr);
            }

            return expr;
        }

        return super.dotRest(expr);
    }

    @Override
    public OracleOrderBy parseOrderBy() {
        if (getLexer().equalToken(Token.ORDER)) {
            OracleOrderBy orderBy = new OracleOrderBy();

            getLexer().nextToken();

            if (getLexer().identifierEquals("SIBLINGS")) {
                getLexer().nextToken();
                orderBy.setSibings(true);
            }

            accept(Token.BY);

            orderBy.addItem(parseSelectOrderByItem());

            while (getLexer().equalToken(Token.COMMA)) {
                getLexer().nextToken();
                orderBy.addItem(parseSelectOrderByItem());
            }

            return orderBy;
        }

        return null;
    }

    protected SQLAggregateExpr parseAggregateExpr(String methodName) {
        methodName = methodName.toUpperCase();
        
        SQLAggregateExpr aggregateExpr;
        if (getLexer().equalToken(Token.UNIQUE)) {
            aggregateExpr = new SQLAggregateExpr(methodName, SQLAggregateOption.UNIQUE);
            getLexer().nextToken();
        } else if (getLexer().equalToken(Token.ALL)) {
            aggregateExpr = new SQLAggregateExpr(methodName, SQLAggregateOption.ALL);
            getLexer().nextToken();
        } else if (getLexer().equalToken(Token.DISTINCT)) {
            aggregateExpr = new SQLAggregateExpr(methodName, SQLAggregateOption.DISTINCT);
            getLexer().nextToken();
        } else {
            aggregateExpr = new SQLAggregateExpr(methodName);
        }
        exprList(aggregateExpr.getArguments(), aggregateExpr);

        if (getLexer().getLiterals().equalsIgnoreCase("IGNORE")) {
            getLexer().nextToken();
            getLexer().identifierEquals("NULLS");
            aggregateExpr.setIgnoreNulls(true);
        }

        accept(Token.RIGHT_PAREN);
        
        if (getLexer().identifierEquals("WITHIN")) {
            getLexer().nextToken();
            accept(Token.GROUP);
            accept(Token.LEFT_PAREN);
            SQLOrderBy withinGroup = this.parseOrderBy();
            aggregateExpr.setWithinGroup(withinGroup);
            accept(Token.RIGHT_PAREN);
        }

        if (getLexer().equalToken(Token.OVER)) {
            OracleAnalytic over = new OracleAnalytic();

            getLexer().nextToken();
            accept(Token.LEFT_PAREN);

            if (getLexer().identifierEquals("PARTITION")) {
                getLexer().nextToken();
                accept(Token.BY);

                if (getLexer().equalToken(Token.LEFT_PAREN)) {
                    getLexer().nextToken();
                    exprList(over.getPartitionBy(), over);
                    accept(Token.RIGHT_PAREN);
                } else {
                    exprList(over.getPartitionBy(), over);
                }
            }

            over.setOrderBy(parseOrderBy());
            if (over.getOrderBy() != null) {
                OracleAnalyticWindowing windowing = null;
                if (getLexer().getLiterals().equalsIgnoreCase("ROWS")) {
                    getLexer().nextToken();
                    windowing = new OracleAnalyticWindowing();
                    windowing.setType(OracleAnalyticWindowing.Type.ROWS);
                } else if (getLexer().getLiterals().equalsIgnoreCase("RANGE")) {
                    getLexer().nextToken();
                    windowing = new OracleAnalyticWindowing();
                    windowing.setType(OracleAnalyticWindowing.Type.RANGE);
                }

                if (windowing != null) {
                    if (getLexer().getLiterals().equalsIgnoreCase("CURRENT")) {
                        getLexer().nextToken();
                        if (getLexer().getLiterals().equalsIgnoreCase("ROW")) {
                            getLexer().nextToken();
                            windowing.setExpr(new SQLIdentifierExpr("CURRENT ROW"));
                            over.setWindowing(windowing);
                        }
                        throw new ParserException(getLexer());
                    }
                    if (getLexer().getLiterals().equalsIgnoreCase("UNBOUNDED")) {
                        getLexer().nextToken();
                        if (getLexer().getLiterals().equalsIgnoreCase("PRECEDING")) {
                            getLexer().nextToken();
                            windowing.setExpr(new SQLIdentifierExpr("UNBOUNDED PRECEDING"));
                        } else {
                            throw new ParserException(getLexer());
                        }
                    }

                    over.setWindowing(windowing);
                }
            }

            accept(Token.RIGHT_PAREN);

            aggregateExpr.setOver(over);
        }
        return aggregateExpr;
    }
    
    @Override
    public OracleSelectParser createSelectParser() {
        return new OracleSelectParser(this);
    }
    
    @Override
    public OracleOrderByItem parseSelectOrderByItem() {
        OracleOrderByItem item = new OracleOrderByItem();

        item.setExpr(expr());

        if (getLexer().equalToken(Token.ASC)) {
            getLexer().nextToken();
            item.setType(SQLOrderingSpecification.ASC);
        } else if (getLexer().equalToken(Token.DESC)) {
            getLexer().nextToken();
            item.setType(SQLOrderingSpecification.DESC);
        }

        if (getLexer().identifierEquals("NULLS")) {
            getLexer().nextToken();
            if (getLexer().identifierEquals("FIRST")) {
                getLexer().nextToken();
                item.setNullsOrderType(OracleOrderByItem.NullsOrderType.NullsFirst);
            } else if (getLexer().identifierEquals("LAST")) {
                getLexer().nextToken();
                item.setNullsOrderType(OracleOrderByItem.NullsOrderType.NullsLast);
            } else {
                throw new ParserUnsupportedException(getLexer().getToken());
            }
        }

        return item;
    }

    protected SQLExpr parseInterval() {
        accept(Token.INTERVAL);
        
        OracleIntervalExpr interval = new OracleIntervalExpr();
        if (!getLexer().equalToken(Token.LITERAL_CHARS)) {
            return new SQLIdentifierExpr("INTERVAL");
        }
        interval.setValue(new SQLCharExpr(getLexer().getLiterals()));
        getLexer().nextToken();

        
        OracleIntervalType type = OracleIntervalType.valueOf(getLexer().getLiterals());
        interval.setType(type);
        getLexer().nextToken();
        
        if (getLexer().equalToken(Token.LEFT_PAREN)) {
            getLexer().nextToken();
            if (!getLexer().equalToken(Token.LITERAL_INT)) {
                throw new ParserException(getLexer());
            }
            interval.setPrecision(getLexer().integerValue().intValue());
            getLexer().nextToken();
            
            if (getLexer().equalToken(Token.COMMA)) {
                getLexer().nextToken();
                if (!getLexer().equalToken(Token.LITERAL_INT)) {
                    throw new ParserException(getLexer());
                }
                interval.setFactionalSecondsPrecision(getLexer().integerValue().intValue());
                getLexer().nextToken();
            }
            accept(Token.RIGHT_PAREN);
        }
        
        if (getLexer().equalToken(Token.TO)) {
            getLexer().nextToken();
            if (getLexer().identifierEquals("SECOND")) {
                getLexer().nextToken();
                interval.setToType(OracleIntervalType.SECOND);
                if (getLexer().equalToken(Token.LEFT_PAREN)) {
                    getLexer().nextToken();
                    if (!getLexer().equalToken(Token.LITERAL_INT)) {
                        throw new ParserException(getLexer());
                    }
                    interval.setToFactionalSecondsPrecision(getLexer().integerValue().intValue());
                    getLexer().nextToken();
                    accept(Token.RIGHT_PAREN);
                }
            } else {
                interval.setToType(OracleIntervalType.MONTH);
                getLexer().nextToken();
            }
        }
        
        return interval;    
    }
    
    public SQLExpr relationalRest(SQLExpr expr) {
        if (getLexer().equalToken(Token.IS)) {
            getLexer().nextToken();
            
            if (getLexer().equalToken(Token.NOT)) {
                getLexer().nextToken();
                SQLExpr rightExpr = primary();
                expr = new SQLBinaryOpExpr(expr, SQLBinaryOperator.IsNot, rightExpr, getDbType());
            } else if (getLexer().identifierEquals("A")) {
                getLexer().nextToken();
                accept(Token.SET);
                expr = new OracleIsSetExpr(expr);
            } else {
                SQLExpr rightExpr = primary();
                expr = new SQLBinaryOpExpr(expr, SQLBinaryOperator.Is, rightExpr, getDbType());
            }
            
            return expr;
        }
        return super.relationalRest(expr);
    }
    
    
    public SQLName name() {
        SQLName name = super.name();
        
        if (getLexer().equalToken(Token.MONKEYS_AT)) {
            getLexer().nextToken();
            if (!getLexer().equalToken(Token.IDENTIFIER)) {
                throw new ParserException(getLexer(), Token.IDENTIFIER);
            }
            OracleDbLinkExpr dbLink = new OracleDbLinkExpr();
            dbLink.setExpr(name);
            dbLink.setDbLink(getLexer().getLiterals());
            getLexer().nextToken();
            return dbLink;
        }
        
        return name;
    }
    
    public SQLExpr equalityRest(SQLExpr expr) {
        SQLExpr rightExp;
        if (getLexer().equalToken(Token.EQ)) {
            getLexer().nextToken();
            
            if (getLexer().equalToken(Token.GT)) {
                getLexer().nextToken();
                rightExp = expr();
                String argumentName = ((SQLIdentifierExpr) expr).getName();
                return new OracleArgumentExpr(argumentName, rightExp);
            }
            
            rightExp = shift();

            rightExp = equalityRest(rightExp);

            expr = new SQLBinaryOpExpr(expr, SQLBinaryOperator.Equality, rightExp, getDbType());
        } else if (getLexer().equalToken(Token.BANG_EQ)) {
            getLexer().nextToken();
            rightExp = shift();

            rightExp = equalityRest(rightExp);

            expr = new SQLBinaryOpExpr(expr, SQLBinaryOperator.NotEqual, rightExp, getDbType());
        }

        return expr;
    }
    
    public OraclePrimaryKey parsePrimaryKey() {
        getLexer().nextToken();
        accept(Token.KEY);

        OraclePrimaryKey primaryKey = new OraclePrimaryKey();
        accept(Token.LEFT_PAREN);
        exprList(primaryKey.getColumns(), primaryKey);
        accept(Token.RIGHT_PAREN);

        
        if (getLexer().equalToken(Token.USING)) {
            OracleUsingIndexClause using = parseUsingIndex();
            primaryKey.setUsing(using);
        }
        return primaryKey;
    }

    private OracleUsingIndexClause parseUsingIndex() {
        accept(Token.USING);
        accept(Token.INDEX);
        
        OracleUsingIndexClause using = new OracleUsingIndexClause();
        
        while (true) {
            if (getLexer().equalToken(Token.TABLESPACE)) {
                getLexer().nextToken();
                using.setTablespace(this.name());
            } else if (getLexer().equalToken(Token.PCTFREE)) {
                getLexer().nextToken();
                using.setPtcfree(this.expr());
            } else if (getLexer().equalToken(Token.INITRANS)) {
                getLexer().nextToken();
                using.setInitrans(this.expr());
            } else if (getLexer().equalToken(Token.MAXTRANS)) {
                getLexer().nextToken();
                using.setMaxtrans(this.expr());
            } else if (getLexer().equalToken(Token.COMPUTE)) {
                getLexer().nextToken();
                acceptIdentifier("STATISTICS");
                using.setComputeStatistics(true);
            } else if (getLexer().equalToken(Token.ENABLE)) {
                getLexer().nextToken();
                using.setEnable(true);
            } else if (getLexer().equalToken(Token.DISABLE)) {
                getLexer().nextToken();
                using.setEnable(false);
            } else if (getLexer().equalToken(Token.STORAGE)) {
                OracleStorageClause storage = parseStorage();
                using.setStorage(storage);
            } else if (getLexer().equalToken(Token.IDENTIFIER)) {
                using.setTablespace(this.name());
                break;
            } else {
                break;
            }
        }
        return using;
    }
    
    public SQLColumnDefinition parseColumnRest(SQLColumnDefinition column) {
        column = super.parseColumnRest(column);
        
        if (getLexer().equalToken(Token.ENABLE)) {
            getLexer().nextToken();
            column.setEnable(Boolean.TRUE);
        }
        
        return column;
    }
    
    public SQLExpr exprRest(SQLExpr expr) {
        expr = super.exprRest(expr);
        
        if (getLexer().equalToken(Token.COLON_EQ)) {
            getLexer().nextToken();
            SQLExpr right = expr();
            expr = new SQLBinaryOpExpr(expr, SQLBinaryOperator.Assignment, right, getDbType());
        }
        
        return expr;
    }
    
    public OracleLobStorageClause parseLobStorage() {
        getLexer().nextToken();
        
        OracleLobStorageClause clause = new OracleLobStorageClause();
        
        accept(Token.LEFT_PAREN);
        this.names(clause.getItems());
        accept(Token.RIGHT_PAREN);
        
        accept(Token.STORE);
        accept(Token.AS);
        
        while (true) {
            if (getLexer().identifierEquals("SECUREFILE")) {
                getLexer().nextToken();
                clause.setSecureFile(true);
                continue;
            }
            
            if (getLexer().identifierEquals("BASICFILE")) {
                getLexer().nextToken();
                clause.setBasicFile(true);
                continue;
            }
            
            if (getLexer().equalToken(Token.LEFT_PAREN)) {
                getLexer().nextToken();
                
                while (true) {
                    if (getLexer().equalToken(Token.TABLESPACE)) {
                        getLexer().nextToken();
                        clause.setTableSpace(this.name());
                        continue;
                    }
                    
                    if (getLexer().equalToken(Token.ENABLE)) {
                        getLexer().nextToken();
                        accept(Token.STORAGE);
                        accept(Token.IN);
                        accept(Token.ROW);
                        clause.setEnable(true);
                        continue;
                    }
                    
                    if (getLexer().equalToken(Token.CHUNK)) {
                        getLexer().nextToken();
                        clause.setChunk(this.primary());
                        continue;
                    }
                    
                    if (getLexer().equalToken(Token.NOCACHE)) {
                        getLexer().nextToken();
                        clause.setCache(false);
                        if (getLexer().equalToken(Token.LOGGING)) {
                            getLexer().nextToken();
                            clause.setLogging(true);
                        }
                        continue;
                    }
                    
                    if (getLexer().equalToken(Token.NOCOMPRESS)) {
                        getLexer().nextToken();
                        clause.setCompress(false);
                        continue;
                    }
                    
                    if (getLexer().equalToken(Token.KEEP_DUPLICATES)) {
                        getLexer().nextToken();
                        clause.setKeepDuplicate(true);
                        continue;
                    }
                    
                    break;
                }
                
                accept(Token.RIGHT_PAREN);
            }
            
            break;
        }
        return clause;
    }
    
    public OracleStorageClause parseStorage() {
        getLexer().nextToken();
        accept(Token.LEFT_PAREN);

        OracleStorageClause storage = new OracleStorageClause();
        while (true) {
            if (getLexer().identifierEquals("INITIAL")) {
                getLexer().nextToken();
                storage.setInitial(this.expr());
                continue;
            } else if (getLexer().equalToken(Token.NEXT)) {
                getLexer().nextToken();
                storage.setNext(this.expr());
                continue;
            } else if (getLexer().equalToken(Token.MINEXTENTS)) {
                getLexer().nextToken();
                storage.setMinExtents(this.expr());
                continue;
            } else if (getLexer().equalToken(Token.MAXEXTENTS)) {
                getLexer().nextToken();
                storage.setMaxExtents(this.expr());
                continue;
            } else if (getLexer().equalToken(Token.MAXSIZE)) {
                getLexer().nextToken();
                storage.setMaxSize(this.expr());
                continue;
            } else if (getLexer().equalToken(Token.PCTINCREASE)) {
                getLexer().nextToken();
                storage.setPctIncrease(this.expr());
                continue;
            } else if (getLexer().identifierEquals("FREELISTS")) {
                getLexer().nextToken();
                storage.setFreeLists(this.expr());
                continue;
            } else if (getLexer().identifierEquals("FREELIST")) {
                getLexer().nextToken();
                acceptIdentifier("GROUPS");
                storage.setFreeListGroups(this.expr());
                continue;
            } else if (getLexer().identifierEquals("BUFFER_POOL")) {
                getLexer().nextToken();
                storage.setBufferPool(this.expr());
                continue;
            } else if (getLexer().identifierEquals("OBJNO")) {
                getLexer().nextToken();
                storage.setObjno(this.expr());
                continue;
            } else if (getLexer().equalToken(Token.FLASH_CACHE)) {
                getLexer().nextToken();
                FlashCacheType flashCacheType;
                if (getLexer().equalToken(Token.KEEP)) {
                    flashCacheType = FlashCacheType.KEEP;
                    getLexer().nextToken();
                } else if (getLexer().equalToken(Token.NONE)) {
                    flashCacheType = FlashCacheType.NONE;
                    getLexer().nextToken();
                } else {
                    accept(Token.DEFAULT);
                    flashCacheType = FlashCacheType.DEFAULT;
                }
                storage.setFlashCache(flashCacheType);
                continue;
            } else if (getLexer().equalToken(Token.CELL_FLASH_CACHE)) {
                getLexer().nextToken();
                FlashCacheType flashCacheType;
                if (getLexer().equalToken(Token.KEEP)) {
                    flashCacheType = FlashCacheType.KEEP;
                    getLexer().nextToken();
                } else if (getLexer().equalToken(Token.NONE)) {
                    flashCacheType = FlashCacheType.NONE;
                    getLexer().nextToken();
                } else {
                    accept(Token.DEFAULT);
                    flashCacheType = FlashCacheType.DEFAULT;
                }
                storage.setCellFlashCache(flashCacheType);
                continue;
            }

            break;
        }
        accept(Token.RIGHT_PAREN);
        return storage;
    }
    
    public SQLUnique parseUnique() {
        accept(Token.UNIQUE);

        OracleUnique unique = new OracleUnique();
        accept(Token.LEFT_PAREN);
        exprList(unique.getColumns(), unique);
        accept(Token.RIGHT_PAREN);
        
        if (getLexer().equalToken(Token.USING)) {
            OracleUsingIndexClause using = parseUsingIndex();
            unique.setUsing(using);
        }

        return unique;
    }
    
    public OracleConstraint parseConstraint() {
        OracleConstraint constraint = (OracleConstraint) super.parseConstraint();
        
        while (true) {
            if (getLexer().equalToken(Token.EXCEPTIONS)) {
                getLexer().nextToken();
                accept(Token.INTO);
                SQLName exceptionsInto = this.name();
                constraint.setExceptionsInto(exceptionsInto);
                continue;
            }
            
            if (getLexer().equalToken(Token.DISABLE)) {
                getLexer().nextToken();
                constraint.setEnable(false);
                continue;
            }
            
            if (getLexer().equalToken(Token.ENABLE)) {
                getLexer().nextToken();
                constraint.setEnable(true);
                continue;
            }
            
            if (getLexer().equalToken(Token.INITIALLY)) {
                getLexer().nextToken();
                
                if (getLexer().equalToken(Token.IMMEDIATE)) {
                    getLexer().nextToken();
                    constraint.setInitially(Initially.IMMEDIATE);
                } else {
                    accept(Token.DEFERRED);
                    constraint.setInitially(Initially.DEFERRED);
                }
                
                continue;
            }
            
            if (getLexer().equalToken(Token.NOT)) {
                getLexer().nextToken();
                if (getLexer().identifierEquals("DEFERRABLE")) {
                    getLexer().nextToken();
                    constraint.setDeferrable(false);
                    continue;
                }
                throw new ParserUnsupportedException(getLexer().getToken());
            }
            
            if (getLexer().identifierEquals("DEFERRABLE")) {
                getLexer().nextToken();
                constraint.setDeferrable(true);
                continue;
            }
            break;
        }
        return constraint;
    }
    
    protected SQLForeignKeyConstraint createForeignKey() {
        return new OracleForeignKey();
    }
    
    protected SQLCheck createCheck() {
        return new OracleCheck();
    }
}
