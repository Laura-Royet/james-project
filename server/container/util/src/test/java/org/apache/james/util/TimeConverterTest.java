/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class TimeConverterTest {

    @Test 
    public void getMilliSecondsShouldReturnMilliSecondFromMilliSecondWhenAmountAndUnit() { 
        //Given
        long AMOUNT = 2;
        long expected = 2;
        String unit = "msec";
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT, unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
        
    @Test 
    public void getMilliSecondsShouldReturnMilliSecondFromMilliSecondWhenRawString() { 
        //Given
        long AMOUNT = 2;
        long expected = 2;
        String unit = "msec"; 
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT + " " + unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
   
    @Test
    public void getMilliSecondsShouldReturnMilliSecondFromMilliSecondsWhenAmountAndUnit() { 
        //Given
        long AMOUNT = 2;
        long expected = 2;
        String unit = "msecs";
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT, unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test
    public void getMilliSecondsShouldReturnMilliSecondFromMilliSecondsWhenRawString() {
        //Given
        long AMOUNT = 2;
        long expected = 2;
        String unit = "msecs";
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT + " " + unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test 
    public void getMilliSecondsShouldReturnMilliSecondFromSecondWhenAmountAndUnit() { 
        //Given
        long AMOUNT = 2;
        long expected = 2000;
        String unit = "sec";
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT, unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test 
    public void getMilliSecondsShouldReturnMilliSecondFromSecondWhenRawString() { 
        //Given
        long AMOUNT = 2;
        long expected = 2000;
        String unit = "sec";
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT + " " + unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test 
    public void getMilliSecondsShouldReturnMilliSecondFromSecondsWhenAmountAndUnit() { 
        //Given
        long AMOUNT = 2;
        long expected = 2000;
        String unit = "secs";
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT, unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test 
    public void getMilliSecondsShouldReturnMilliSecondFromSecondsWhenRawString() { 
        //Given
        long AMOUNT = 2;
        long expected = 2000;
        String unit = "secs";
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT + " " + unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test 
    public void getMilliSecondsShouldReturnMilliSecondFromMinuteWhenAmountAndUnit() { 
        //Given
        long AMOUNT = 2;
        long expected = 120000;
        String unit = "minute";
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT, unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
        
    @Test 
    public void getMilliSecondsShouldReturnMilliSecondFromMinuteWhenRawString() { 
        //Given
        long AMOUNT = 2;
        long expected = 120000;
        String unit = "minute"; 
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT + " " + unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void getMilliSecondsShouldReturnMilliSecondFromMinutesWhenAmountAndUnit() { 
        //Given
        long AMOUNT = 2;
        long expected = 120000;
        String unit = "minutes";
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT, unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
        
    @Test 
    public void getMilliSecondsShouldReturnMilliSecondFromMinutesWhenRawString() { 
        //Given
        long AMOUNT = 2;
        long expected = 120000;
        String unit = "minutes"; 
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT + " " + unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test
    public void getMilliSecondsShouldReturnMilliSecondFromHourWhenAmountAndUnit() { 
        //Given
        long AMOUNT = 2;
        long expected = 7200000;
        String unit = "hour";
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT, unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
        
    @Test
    public void getMilliSecondsShouldReturnMilliSecondFromHourWhenRawString() { 
        //Given
        long AMOUNT = 2;
        long expected = 7200000;
        String unit = "hour"; 
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT + " " + unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test
    public void getMilliSecondsShouldReturnMilliSecondFromHoursWhenAmountAndUnit() { 
        //Given
        long AMOUNT = 2;
        long expected = 7200000;
        String unit = "hours";
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT, unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
        
    @Test 
    public void getMilliSecondsShouldReturnMilliSecondFromHoursWhenRawString() { 
        //Given
        long AMOUNT = 2;
        long expected = 7200000;
        String unit = "hours"; 
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT + " " + unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test
    public void getMilliSecondsShouldReturnMilliSecondFromDayWhenAmountAndUnit() { 
        //Given
        long AMOUNT = 2;
        long expected = 172800000;
        String unit = "day";
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT, unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
        
    @Test
    public void getMilliSecondsShouldReturnMilliSecondFromDayWhenRawString() { 
        //Given
        long AMOUNT = 2;
        long expected = 172800000;
        String unit = "day"; 
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT + " " + unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test
    public void getMilliSecondsShouldReturnMilliSecondFromDaysWhenAmountAndUnit() { 
        //Given
        long AMOUNT = 2;
        long expected = 172800000;
        String unit = "days";
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT, unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
        
    @Test 
    public void getMilliSecondsShouldReturnMilliSecondFromDaysWhenRawString() { 
        //Given
        long AMOUNT = 2;
        long expected = 172800000;
        String unit = "days"; 
        //When
        long actual = TimeConverter.getMilliSeconds(AMOUNT + " " + unit);
        //Then
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test(expected = NumberFormatException.class) 
    public void getMilliSecondsShouldThrowWhenIllegalUnitInUnit() {
        TimeConverter.getMilliSeconds(2, " week");
    } 
    
    @Test(expected = NumberFormatException.class) 
    public void getMilliSecondsShouldThrowWhenIllegalUnitInRawString() { 
        TimeConverter.getMilliSeconds(2 + " week");
    } 

    @Test (expected = NumberFormatException.class)
    public void getMilliSecondsShouldThrowWhenIllegalPattern() {
        TimeConverter.getMilliSeconds("illegal pattern");
    }
}