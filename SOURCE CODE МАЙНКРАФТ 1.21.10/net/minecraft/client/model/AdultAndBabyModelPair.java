package net.minecraft.client.model;

public record AdultAndBabyModelPair<T extends Model>(T adultModel, T babyModel) {
   public AdultAndBabyModelPair(T param1, T param2) {
      super();
      this.adultModel = var1;
      this.babyModel = var2;
   }

   public T getModel(boolean var1) {
      return var1 ? this.babyModel : this.adultModel;
   }

   public T adultModel() {
      return this.adultModel;
   }

   public T babyModel() {
      return this.babyModel;
   }
}
