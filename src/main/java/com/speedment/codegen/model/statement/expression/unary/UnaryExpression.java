package com.speedment.codegen.model.statement.expression.unary;

import com.speedment.codegen.model.Operator_;
import com.speedment.codegen.model.statement.expression.Expression;
import com.speedment.codegen.model.statement.expression.DefaultExpression;

/**
 *
 * @author pemi
 * @param <T>
 */
public class UnaryExpression<T extends UnaryExpression<T>> extends DefaultExpression<T> {

    private Expression expression;

    public UnaryExpression() {
    }

    public UnaryExpression(Operator_ operator) {
        super(operator);
    }

    public UnaryExpression(Operator_ operator, Expression statement) {
        setOperator(operator);
        this.expression = statement;
    }

    public Expression get() {
        return expression;
    }

    public T set(Expression expression) {
        return set(expression, e -> this.expression = e);
    }

}
