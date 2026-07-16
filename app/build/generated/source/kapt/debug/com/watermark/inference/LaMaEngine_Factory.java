package com.watermark.inference;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class LaMaEngine_Factory implements Factory<LaMaEngine> {
  private final Provider<Context> contextProvider;

  public LaMaEngine_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public LaMaEngine get() {
    return newInstance(contextProvider.get());
  }

  public static LaMaEngine_Factory create(Provider<Context> contextProvider) {
    return new LaMaEngine_Factory(contextProvider);
  }

  public static LaMaEngine newInstance(Context context) {
    return new LaMaEngine(context);
  }
}
