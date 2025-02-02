package io.javaoperatorsdk.operator.api.config;

import java.lang.reflect.ParameterizedType;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.ReconcilerUtils;
import io.javaoperatorsdk.operator.api.reconciler.Constants;
import io.javaoperatorsdk.operator.processing.event.source.controller.ResourceEventFilter;
import io.javaoperatorsdk.operator.processing.event.source.controller.ResourceEventFilters;

public interface ControllerConfiguration<R extends HasMetadata> {

  default String getName() {
    return ReconcilerUtils.getDefaultReconcilerName(getAssociatedReconcilerClassName());
  }

  default String getResourceTypeName() {
    return ReconcilerUtils.getResourceTypeName(getResourceClass());
  }

  default String getFinalizer() {
    return ReconcilerUtils.getDefaultFinalizerName(getResourceClass());
  }

  /**
   * Retrieves the label selector that is used to filter which custom resources are actually watched
   * by the associated controller. See
   * https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/ for more details on
   * syntax.
   *
   * @return the label selector filtering watched custom resources
   */
  default String getLabelSelector() {
    return null;
  }

  default boolean isGenerationAware() {
    return true;
  }

  default Class<R> getResourceClass() {
    ParameterizedType type = (ParameterizedType) getClass().getGenericInterfaces()[0];
    return (Class<R>) type.getActualTypeArguments()[0];
  }

  String getAssociatedReconcilerClassName();

  default Set<String> getNamespaces() {
    return Collections.emptySet();
  }

  default boolean watchAllNamespaces() {
    return allNamespacesWatched(getNamespaces());
  }

  static boolean allNamespacesWatched(Set<String> namespaces) {
    return namespaces == null || namespaces.isEmpty();
  }

  default boolean watchCurrentNamespace() {
    return currentNamespaceWatched(getNamespaces());
  }

  static boolean currentNamespaceWatched(Set<String> namespaces) {
    return namespaces != null
        && namespaces.size() == 1
        && namespaces.contains(Constants.WATCH_CURRENT_NAMESPACE);
  }

  /**
   * Computes the effective namespaces based on the set specified by the user, in particular
   * retrieves the current namespace from the client when the user specified that they wanted to
   * watch the current namespace only.
   *
   * @return a Set of namespace names the associated controller will watch
   */
  default Set<String> getEffectiveNamespaces() {
    var targetNamespaces = getNamespaces();
    if (watchCurrentNamespace()) {
      final var parent = getConfigurationService();
      if (parent == null) {
        throw new IllegalStateException(
            "Parent ConfigurationService must be set before calling this method");
      }
      targetNamespaces = Collections.singleton(parent.getClientConfiguration().getNamespace());
    }
    return targetNamespaces;
  }

  default RetryConfiguration getRetryConfiguration() {
    return RetryConfiguration.DEFAULT;
  }

  ConfigurationService getConfigurationService();

  default void setConfigurationService(ConfigurationService service) {}

  default boolean useFinalizer() {
    return !Constants.NO_FINALIZER.equals(getFinalizer());
  }

  /**
   * Allow controllers to filter events before they are passed to the
   * {@link io.javaoperatorsdk.operator.processing.event.EventHandler}.
   *
   * <p>
   * Resource event filters only applies on events of the main custom resource. Not on events from
   * other event sources nor the periodic events.
   * </p>
   *
   * @return filter
   */
  default ResourceEventFilter<R> getEventFilter() {
    return ResourceEventFilters.passthrough();
  }

  default Optional<Duration> reconciliationMaxInterval() {
    return Optional.of(Duration.ofHours(10L));
  }
}
