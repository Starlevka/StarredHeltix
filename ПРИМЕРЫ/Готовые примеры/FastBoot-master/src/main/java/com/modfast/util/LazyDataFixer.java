package com.modfast.util;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.serialization.Dynamic;
import com.mojang.datafixers.schemas.Schema;

import java.util.function.Supplier;

public class LazyDataFixer implements DataFixer {
    private final Supplier<DataFixer> fixerSupplier;
    private DataFixer resolved;

    public LazyDataFixer(Supplier<DataFixer> fixerSupplier) {
        this.fixerSupplier = fixerSupplier;
    }

    private synchronized DataFixer get() {
        if (this.resolved == null) {
            this.resolved = this.fixerSupplier.get();
        }
        return this.resolved;
    }

    @Override
    public <T> Dynamic<T> update(TypeReference type, Dynamic<T> input, int version, int newVersion) {
        return this.get().update(type, input, version, newVersion);
    }

    @Override
    public Schema getSchema(int key) {
        return this.get().getSchema(key);
    }
}
