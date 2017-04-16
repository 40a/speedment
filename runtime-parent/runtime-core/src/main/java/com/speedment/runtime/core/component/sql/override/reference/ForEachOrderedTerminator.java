package com.speedment.runtime.core.component.sql.override.reference;

import com.speedment.runtime.core.component.sql.SqlStreamOptimizerInfo;
import static com.speedment.runtime.core.internal.component.sql.override.def.reference.DefaultForEachOrderedTerminator.DEFAULT;
import com.speedment.runtime.core.internal.manager.sql.SqlStreamTerminator;
import com.speedment.runtime.core.internal.stream.builder.pipeline.ReferencePipeline;
import java.util.function.Consumer;

/**
 *
 * @author Per Minborg
 * @param <ENTITY> the original stream entity source type 
 */
@FunctionalInterface
public interface ForEachOrderedTerminator<ENTITY> extends ReferenceTerminator {

    <T> void apply(
        SqlStreamOptimizerInfo<ENTITY> info,
        SqlStreamTerminator<ENTITY> sqlStreamTerminator,
        ReferencePipeline<T> pipeline,
        Consumer<? super T> action
    );

    @SuppressWarnings("unchecked")
    static <ENTITY> ForEachOrderedTerminator<ENTITY> defaultTerminator() {
        return (ForEachOrderedTerminator<ENTITY>) DEFAULT;
    }

}