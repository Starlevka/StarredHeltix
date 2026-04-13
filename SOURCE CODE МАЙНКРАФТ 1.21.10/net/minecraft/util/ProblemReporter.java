package net.minecraft.util;

import com.google.common.collect.HashMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;

public interface ProblemReporter {
   ProblemReporter DISCARDING = new ProblemReporter() {
      public ProblemReporter forChild(ProblemReporter.PathElement var1) {
         return this;
      }

      public void report(ProblemReporter.Problem var1) {
      }
   };

   ProblemReporter forChild(ProblemReporter.PathElement var1);

   void report(ProblemReporter.Problem var1);

   public static class ScopedCollector extends ProblemReporter.Collector implements AutoCloseable {
      private final Logger logger;

      public ScopedCollector(Logger var1) {
         super();
         this.logger = var1;
      }

      public ScopedCollector(ProblemReporter.PathElement var1, Logger var2) {
         super(var1);
         this.logger = var2;
      }

      public void close() {
         if (!this.isEmpty()) {
            this.logger.warn("[{}] Serialization errors:\n{}", this.logger.getName(), this.getTreeReport());
         }

      }
   }

   public static class Collector implements ProblemReporter {
      public static final ProblemReporter.PathElement EMPTY_ROOT = () -> {
         return "";
      };
      @Nullable
      private final ProblemReporter.Collector parent;
      private final ProblemReporter.PathElement element;
      private final Set<ProblemReporter.Collector.Entry> problems;

      public Collector() {
         this(EMPTY_ROOT);
      }

      public Collector(ProblemReporter.PathElement var1) {
         super();
         this.parent = null;
         this.problems = new LinkedHashSet();
         this.element = var1;
      }

      private Collector(ProblemReporter.Collector var1, ProblemReporter.PathElement var2) {
         super();
         this.problems = var1.problems;
         this.parent = var1;
         this.element = var2;
      }

      public ProblemReporter forChild(ProblemReporter.PathElement var1) {
         return new ProblemReporter.Collector(this, var1);
      }

      public void report(ProblemReporter.Problem var1) {
         this.problems.add(new ProblemReporter.Collector.Entry(this, var1));
      }

      public boolean isEmpty() {
         return this.problems.isEmpty();
      }

      public void forEach(BiConsumer<String, ProblemReporter.Problem> var1) {
         ArrayList var2 = new ArrayList();
         StringBuilder var3 = new StringBuilder();
         Iterator var4 = this.problems.iterator();

         while(var4.hasNext()) {
            ProblemReporter.Collector.Entry var5 = (ProblemReporter.Collector.Entry)var4.next();

            for(ProblemReporter.Collector var6 = var5.source; var6 != null; var6 = var6.parent) {
               var2.add(var6.element);
            }

            for(int var7 = var2.size() - 1; var7 >= 0; --var7) {
               var3.append(((ProblemReporter.PathElement)var2.get(var7)).get());
            }

            var1.accept(var3.toString(), var5.problem());
            var3.setLength(0);
            var2.clear();
         }

      }

      public String getReport() {
         HashMultimap var1 = HashMultimap.create();
         Objects.requireNonNull(var1);
         this.forEach(var1::put);
         return (String)var1.asMap().entrySet().stream().map((var0) -> {
            String var10000 = (String)var0.getKey();
            return " at " + var10000 + ": " + (String)((Collection)var0.getValue()).stream().map(ProblemReporter.Problem::description).collect(Collectors.joining("; "));
         }).collect(Collectors.joining("\n"));
      }

      public String getTreeReport() {
         ArrayList var1 = new ArrayList();
         ProblemReporter.Collector.ProblemTreeNode var2 = new ProblemReporter.Collector.ProblemTreeNode(this.element);
         Iterator var3 = this.problems.iterator();

         while(var3.hasNext()) {
            ProblemReporter.Collector.Entry var4 = (ProblemReporter.Collector.Entry)var3.next();

            for(ProblemReporter.Collector var5 = var4.source; var5 != this; var5 = var5.parent) {
               var1.add(var5.element);
            }

            ProblemReporter.Collector.ProblemTreeNode var6 = var2;

            for(int var7 = var1.size() - 1; var7 >= 0; --var7) {
               var6 = var6.child((ProblemReporter.PathElement)var1.get(var7));
            }

            var1.clear();
            var6.problems.add(var4.problem);
         }

         return String.join("\n", var2.getLines());
      }

      private static record Entry(ProblemReporter.Collector source, ProblemReporter.Problem problem) {
         final ProblemReporter.Collector source;
         final ProblemReporter.Problem problem;

         Entry(ProblemReporter.Collector param1, ProblemReporter.Problem param2) {
            super();
            this.source = var1;
            this.problem = var2;
         }

         public ProblemReporter.Collector source() {
            return this.source;
         }

         public ProblemReporter.Problem problem() {
            return this.problem;
         }
      }

      private static record ProblemTreeNode(ProblemReporter.PathElement element, List<ProblemReporter.Problem> problems, Map<ProblemReporter.PathElement, ProblemReporter.Collector.ProblemTreeNode> children) {
         final List<ProblemReporter.Problem> problems;

         public ProblemTreeNode(ProblemReporter.PathElement var1) {
            this(var1, new ArrayList(), new LinkedHashMap());
         }

         private ProblemTreeNode(ProblemReporter.PathElement param1, List<ProblemReporter.Problem> param2, Map<ProblemReporter.PathElement, ProblemReporter.Collector.ProblemTreeNode> param3) {
            super();
            this.element = var1;
            this.problems = var2;
            this.children = var3;
         }

         public ProblemReporter.Collector.ProblemTreeNode child(ProblemReporter.PathElement var1) {
            return (ProblemReporter.Collector.ProblemTreeNode)this.children.computeIfAbsent(var1, ProblemReporter.Collector.ProblemTreeNode::new);
         }

         public List<String> getLines() {
            int var1 = this.problems.size();
            int var2 = this.children.size();
            if (var1 == 0 && var2 == 0) {
               return List.of();
            } else {
               ArrayList var3;
               if (var1 == 0 && var2 == 1) {
                  var3 = new ArrayList();
                  this.children.forEach((var1x, var2x) -> {
                     var3.addAll(var2x.getLines());
                  });
                  String var10002 = this.element.get();
                  var3.set(0, var10002 + (String)var3.get(0));
                  return var3;
               } else if (var1 == 1 && var2 == 0) {
                  String var10000 = this.element.get();
                  return List.of(var10000 + ": " + ((ProblemReporter.Problem)this.problems.getFirst()).description());
               } else {
                  var3 = new ArrayList();
                  this.children.forEach((var1x, var2x) -> {
                     var3.addAll(var2x.getLines());
                  });
                  var3.replaceAll((var0) -> {
                     return "  " + var0;
                  });
                  Iterator var4 = this.problems.iterator();

                  while(var4.hasNext()) {
                     ProblemReporter.Problem var5 = (ProblemReporter.Problem)var4.next();
                     var3.add("  " + var5.description());
                  }

                  var3.addFirst(this.element.get() + ":");
                  return var3;
               }
            }
         }

         public ProblemReporter.PathElement element() {
            return this.element;
         }

         public List<ProblemReporter.Problem> problems() {
            return this.problems;
         }

         public Map<ProblemReporter.PathElement, ProblemReporter.Collector.ProblemTreeNode> children() {
            return this.children;
         }
      }
   }

   public static record ElementReferencePathElement(ResourceKey<?> id) implements ProblemReporter.PathElement {
      public ElementReferencePathElement(ResourceKey<?> param1) {
         super();
         this.id = var1;
      }

      public String get() {
         String var10000 = String.valueOf(this.id.location());
         return "->{" + var10000 + "@" + String.valueOf(this.id.registry()) + "}";
      }

      public ResourceKey<?> id() {
         return this.id;
      }
   }

   public static record IndexedPathElement(int index) implements ProblemReporter.PathElement {
      public IndexedPathElement(int param1) {
         super();
         this.index = var1;
      }

      public String get() {
         return "[" + this.index + "]";
      }

      public int index() {
         return this.index;
      }
   }

   public static record IndexedFieldPathElement(String name, int index) implements ProblemReporter.PathElement {
      public IndexedFieldPathElement(String param1, int param2) {
         super();
         this.name = var1;
         this.index = var2;
      }

      public String get() {
         return "." + this.name + "[" + this.index + "]";
      }

      public String name() {
         return this.name;
      }

      public int index() {
         return this.index;
      }
   }

   public static record FieldPathElement(String name) implements ProblemReporter.PathElement {
      public FieldPathElement(String param1) {
         super();
         this.name = var1;
      }

      public String get() {
         return "." + this.name;
      }

      public String name() {
         return this.name;
      }
   }

   public static record RootElementPathElement(ResourceKey<?> id) implements ProblemReporter.PathElement {
      public RootElementPathElement(ResourceKey<?> param1) {
         super();
         this.id = var1;
      }

      public String get() {
         String var10000 = String.valueOf(this.id.location());
         return "{" + var10000 + "@" + String.valueOf(this.id.registry()) + "}";
      }

      public ResourceKey<?> id() {
         return this.id;
      }
   }

   public static record RootFieldPathElement(String name) implements ProblemReporter.PathElement {
      public RootFieldPathElement(String param1) {
         super();
         this.name = var1;
      }

      public String get() {
         return this.name;
      }

      public String name() {
         return this.name;
      }
   }

   @FunctionalInterface
   public interface PathElement {
      String get();
   }

   public interface Problem {
      String description();
   }
}
