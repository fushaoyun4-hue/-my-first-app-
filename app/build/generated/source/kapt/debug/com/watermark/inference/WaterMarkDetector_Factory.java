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
public final class WaterMarkDetector_Factory implements Factory<WaterMarkDetector> {
  private final Provider<Context> contextProvider;

  public WaterMarkDetector_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public WaterMarkDetector get() {
    return newInstance(contextProvider.get());
  }

  public static WaterMarkDetector_Factory create(Provider<Context> contextProvider) {
    return new WaterMarkDetector_Factory(contextProvider);
  }

  public static WaterMarkDetector newInstance(Context context) {
    return new WaterMarkDetector(context);
  }
}
