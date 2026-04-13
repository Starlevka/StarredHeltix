package net.minecraft.commands.functions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class MacroFunction<T extends ExecutionCommandSource<T>> implements CommandFunction<T> {
   private static final DecimalFormat DECIMAL_FORMAT = (DecimalFormat)Util.make(new DecimalFormat("#"), (var0) -> {
      var0.setMaximumFractionDigits(15);
      var0.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
   });
   private static final int MAX_CACHE_ENTRIES = 8;
   private final List<String> parameters;
   private final Object2ObjectLinkedOpenHashMap<List<String>, InstantiatedFunction<T>> cache = new Object2ObjectLinkedOpenHashMap(8, 0.25F);
   private final ResourceLocation id;
   private final List<MacroFunction.Entry<T>> entries;

   public MacroFunction(ResourceLocation var1, List<MacroFunction.Entry<T>> var2, List<String> var3) {
      super();
      this.id = var1;
      this.entries = var2;
      this.parameters = var3;
   }

   public ResourceLocation id() {
      return this.id;
   }

   public InstantiatedFunction<T> instantiate(@Nullable CompoundTag var1, CommandDispatcher<T> var2) throws FunctionInstantiationException {
      if (var1 == null) {
         throw new FunctionInstantiationException(Component.translatable("commands.function.error.missing_arguments", Component.translationArg(this.id())));
      } else {
         ArrayList var3 = new ArrayList(this.parameters.size());
         Iterator var4 = this.parameters.iterator();

         while(var4.hasNext()) {
            String var5 = (String)var4.next();
            Tag var6 = var1.get(var5);
            if (var6 == null) {
               throw new FunctionInstantiationException(Component.translatable("commands.function.error.missing_argument", Component.translationArg(this.id()), var5));
            }

            var3.add(stringify(var6));
         }

         InstantiatedFunction var7 = (InstantiatedFunction)this.cache.getAndMoveToLast(var3);
         if (var7 != null) {
            return var7;
         } else {
            if (this.cache.size() >= 8) {
               this.cache.removeFirst();
            }

            InstantiatedFunction var8 = this.substituteAndParse(this.parameters, var3, var2);
            this.cache.put(var3, var8);
            return var8;
         }
      }
   }

   private static String stringify(Tag var0) {
      Objects.requireNonNull(var0);
      byte var2 = 0;
      boolean var10001;
      Throwable var33;
      String var34;
      switch(var0.typeSwitch<invokedynamic>(var0, var2)) {
      case 0:
         FloatTag var3 = (FloatTag)var0;
         FloatTag var26 = var3;

         float var27;
         try {
            var27 = var26.value();
         } catch (Throwable var23) {
            var33 = var23;
            var10001 = false;
            break;
         }

         float var28 = var27;
         var34 = DECIMAL_FORMAT.format((double)var28);
         return var34;
      case 1:
         DoubleTag var5 = (DoubleTag)var0;
         DoubleTag var24 = var5;

         double var25;
         try {
            var25 = var24.value();
         } catch (Throwable var22) {
            var33 = var22;
            var10001 = false;
            break;
         }

         double var29 = var25;
         var34 = DECIMAL_FORMAT.format(var29);
         return var34;
      case 2:
         ByteTag var8 = (ByteTag)var0;
         ByteTag var39 = var8;

         byte var40;
         try {
            var40 = var39.value();
         } catch (Throwable var21) {
            var33 = var21;
            var10001 = false;
            break;
         }

         byte var30 = var40;
         var34 = String.valueOf(var30);
         return var34;
      case 3:
         ShortTag var10 = (ShortTag)var0;
         ShortTag var37 = var10;

         short var38;
         try {
            var38 = var37.value();
         } catch (Throwable var20) {
            var33 = var20;
            var10001 = false;
            break;
         }

         short var31 = var38;
         var34 = String.valueOf(var31);
         return var34;
      case 4:
         LongTag var12 = (LongTag)var0;
         LongTag var35 = var12;

         long var36;
         try {
            var36 = var35.value();
         } catch (Throwable var19) {
            var33 = var19;
            var10001 = false;
            break;
         }

         long var32 = var36;
         var34 = String.valueOf(var32);
         return var34;
      case 5:
         StringTag var15 = (StringTag)var0;
         StringTag var10000 = var15;

         try {
            var34 = var10000.value();
         } catch (Throwable var18) {
            var33 = var18;
            var10001 = false;
            break;
         }

         String var17 = var34;
         var34 = var17;
         return var34;
      default:
         var34 = var0.toString();
         return var34;
      }

      Throwable var1 = var33;
      throw new MatchException(var1.toString(), var1);
   }

   private static void lookupValues(List<String> var0, IntList var1, List<String> var2) {
      var2.clear();
      var1.forEach((var2x) -> {
         var2.add((String)var0.get(var2x));
      });
   }

   private InstantiatedFunction<T> substituteAndParse(List<String> var1, List<String> var2, CommandDispatcher<T> var3) throws FunctionInstantiationException {
      ArrayList var4 = new ArrayList(this.entries.size());
      ArrayList var5 = new ArrayList(var2.size());
      Iterator var6 = this.entries.iterator();

      while(var6.hasNext()) {
         MacroFunction.Entry var7 = (MacroFunction.Entry)var6.next();
         lookupValues(var2, var7.parameters(), var5);
         var4.add(var7.instantiate(var5, var3, this.id));
      }

      return new PlainTextFunction(this.id().withPath((var1x) -> {
         return var1x + "/" + var1.hashCode();
      }), var4);
   }

   interface Entry<T> {
      IntList parameters();

      UnboundEntryAction<T> instantiate(List<String> var1, CommandDispatcher<T> var2, ResourceLocation var3) throws FunctionInstantiationException;
   }

   static class MacroEntry<T extends ExecutionCommandSource<T>> implements MacroFunction.Entry<T> {
      private final StringTemplate template;
      private final IntList parameters;
      private final T compilationContext;

      public MacroEntry(StringTemplate var1, IntList var2, T var3) {
         super();
         this.template = var1;
         this.parameters = var2;
         this.compilationContext = var3;
      }

      public IntList parameters() {
         return this.parameters;
      }

      public UnboundEntryAction<T> instantiate(List<String> var1, CommandDispatcher<T> var2, ResourceLocation var3) throws FunctionInstantiationException {
         String var4 = this.template.substitute(var1);

         try {
            return CommandFunction.parseCommand(var2, this.compilationContext, new StringReader(var4));
         } catch (CommandSyntaxException var6) {
            throw new FunctionInstantiationException(Component.translatable("commands.function.error.parse", Component.translationArg(var3), var4, var6.getMessage()));
         }
      }
   }

   static class PlainTextEntry<T> implements MacroFunction.Entry<T> {
      private final UnboundEntryAction<T> compiledAction;

      public PlainTextEntry(UnboundEntryAction<T> var1) {
         super();
         this.compiledAction = var1;
      }

      public IntList parameters() {
         return IntLists.emptyList();
      }

      public UnboundEntryAction<T> instantiate(List<String> var1, CommandDispatcher<T> var2, ResourceLocation var3) {
         return this.compiledAction;
      }
   }
}
