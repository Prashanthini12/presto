/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.nativeworker;

import com.facebook.presto.testing.QueryRunner;
import com.facebook.presto.tests.AbstractTestQueryFramework;
import org.testng.annotations.Test;

import static com.facebook.presto.nativeworker.NativeQueryRunnerUtils.createLineitem;
import static com.facebook.presto.nativeworker.NativeQueryRunnerUtils.createOrdersEx;

public abstract class AbstractTestNativeArrayFunctionQueries
        extends AbstractTestQueryFramework
{
    @Override
    protected void createTables()
    {
        QueryRunner queryRunner = (QueryRunner) getExpectedQueryRunner();
        createLineitem(queryRunner);
        createOrdersEx(queryRunner);
    }

    @Test
    public void testRepeat()
    {
        this.assertQuery("SELECT repeat(orderkey, linenumber) FROM lineitem");
        this.assertQuery("SELECT repeat(orderkey, 3) FROM lineitem");
        this.assertQuery("SELECT repeat(orderkey, NULL) FROM lineitem");
        this.assertQuery("SELECT try(repeat(orderkey, -2)) FROM lineitem");
        this.assertQuery("SELECT try(repeat(orderkey, 10001)) FROM lineitem");
        this.assertQueryFails("SELECT repeat(orderkey, -2) FROM lineitem",
                ".*Count argument of repeat function must be greater than or equal to 0.*");
        this.assertQueryFails("SELECT repeat(orderkey, 10001) FROM lineitem",
                ".*Count argument of repeat function must be less than or equal to 10000.*");
    }

    @Test
    public void testShuffle()
    {
        this.assertQuerySucceeds("SELECT shuffle(quantities) FROM orders_ex");
        this.assertQuerySucceeds("SELECT shuffle(array_sort(quantities)) FROM orders_ex");
        this.assertQuery("SELECT array_sort(shuffle(quantities)) FROM orders_ex");
    }

    @Test
    public void testArrayMatch()
    {
        // test all_match
        this.assertQuery("SELECT all_match(quantities, x -> ((10 / x) > 2)) FROM orders_ex");
        this.assertQuery("SELECT all_match(quantities, x -> TRY(((10 / x) > 2))) FROM orders_ex");
        this.assertQuery("SELECT TRY(all_match(quantities, x -> ((10 / x) > 2))) FROM orders_ex");
        this.assertQuery("SELECT all_match(shuffle(quantities), x -> (x > 500.0)) FROM orders_ex");

        // test any_match
        this.assertQuery("SELECT any_match(quantities, x -> ((10 / x) > 2)) FROM orders_ex");
        this.assertQuery("SELECT any_match(quantities, x -> TRY(((10 / x) > 2))) FROM orders_ex");
        this.assertQuery("SELECT TRY(any_match(quantities, x -> ((10 / x) > 2))) FROM orders_ex");
        this.assertQuery("SELECT any_match(shuffle(quantities), x -> (x > 500.0)) FROM orders_ex");

        // test none_match
        this.assertQuery("SELECT none_match(quantities, x -> ((10 / x) > 2)) FROM orders_ex");
        this.assertQuery("SELECT none_match(quantities, x -> TRY(((10 / x) > 2))) FROM orders_ex");
        this.assertQuery("SELECT TRY(none_match(quantities, x -> ((10 / x) > 2))) FROM orders_ex");
        this.assertQuery("SELECT none_match(shuffle(quantities), x -> (x > 500.0)) FROM orders_ex");
    }

    @Test
    public void testArrayTrim()
    {
        this.assertQuery("SELECT trim_array(quantities, 0) FROM orders_ex");
        this.assertQuery("SELECT trim_array(quantities, 1) FROM orders_ex where cardinality(quantities) > 5");
        this.assertQuery("SELECT trim_array(quantities, 2) FROM orders_ex where cardinality(quantities) > 5");
        this.assertQuery("SELECT trim_array(quantities, 3) FROM orders_ex where cardinality(quantities) > 5");
        this.assertQueryFails("SELECT trim_array(quantities, 3) FROM orders_ex where cardinality(quantities) = 2", ".*size must not exceed array cardinality.*");
    }

    @Test
    public void testArrayConcat()
    {
        // Concatenate two integer arrays.
        this.assertQuery("SELECT concat(ARRAY[linenumber], ARRAY[orderkey, partkey]) FROM lineitem");
        this.assertQuery("SELECT ARRAY[linenumber] || ARRAY[orderkey, partkey] FROM lineitem");
        // Concatenate two integer arrays with null.
        this.assertQuery("SELECT concat(ARRAY[linenumber, NULL], ARRAY[orderkey, partkey]) FROM lineitem");
        this.assertQuery("SELECT concat(ARRAY[linenumber], NULL, ARRAY[orderkey, partkey]) FROM lineitem");
        this.assertQuery("SELECT ARRAY[linenumber, NULL] || ARRAY[orderkey, partkey] FROM lineitem");
        // Concatenate more than two arrays.
        this.assertQuery("SELECT concat(ARRAY[linenumber], ARRAY[partkey], ARRAY[orderkey, partkey], ARRAY[123, 456], ARRAY[quantity]) FROM lineitem");
        this.assertQuery("SELECT ARRAY[linenumber] || ARRAY[partkey] || ARRAY[orderkey, partkey] || ARRAY[123, 456] || ARRAY[quantity] FROM lineitem");
        // Concatenate complex types.
        this.assertQuery("SELECT concat(ARRAY[ARRAY[linenumber], ARRAY[suppkey, orderkey]], ARRAY[ARRAY[orderkey, partkey]]) FROM lineitem");
        this.assertQuery("SELECT ARRAY[ARRAY[linenumber], ARRAY[suppkey, orderkey]] || ARRAY[ARRAY[orderkey, partkey]] FROM lineitem");
        // Concatenate array with a single element.
        this.assertQuery("SELECT concat(linenumber, ARRAY[orderkey, partkey]) FROM lineitem");
        this.assertQuery("SELECT concat(ARRAY[orderkey, partkey], linenumber) FROM lineitem");
        this.assertQuery("SELECT linenumber || ARRAY[orderkey, partkey] FROM lineitem");
        this.assertQuery("SELECT ARRAY[orderkey, partkey] || linenumber FROM lineitem");
        // Concatenate array with a null.
        this.assertQuery("SELECT concat(CAST(NULL AS INTEGER), ARRAY[orderkey, partkey]) FROM lineitem");
        this.assertQuery("SELECT concat(ARRAY[orderkey, partkey], CAST(NULL AS INTEGER)) FROM lineitem");
        this.assertQuery("SELECT CAST(NULL AS INTEGER) || ARRAY[orderkey, partkey] FROM lineitem");
        this.assertQuery("SELECT ARRAY[orderkey, partkey] || CAST(NULL AS INTEGER) FROM lineitem");
        // Test nested concatenation.
        this.assertQuery("SELECT concat(linenumber, concat(orderkey, ARRAY[suppkey, partkey])) FROM lineitem");
        this.assertQuery("SELECT linenumber || concat(orderkey, ARRAY[suppkey, partkey]) FROM lineitem");
        this.assertQuery("SELECT concat(linenumber, orderkey || ARRAY[suppkey, partkey]) FROM lineitem");
    }
}
