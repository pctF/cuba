/*
 * Copyright (c) 2008-2018 Haulmont.
 *
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

package com.haulmont.cuba.gui.screen;

import com.google.common.collect.Iterables;
import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.BeanValidation;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.validation.groups.UiCrossFieldChecks;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.Validator;
import java.util.Collection;
import java.util.Set;

import static com.haulmont.bali.util.Preconditions.checkNotNullArgument;
import static com.haulmont.cuba.gui.screen.UiControllerUtils.getScreenContext;

@Component(ScreenValidation.NAME)
public class ScreenValidation {

    public static final String NAME = "cuba_ScreenValidation";

    @Inject
    protected Configuration configuration;
    @Inject
    protected Messages messages;
    @Inject
    protected Icons icons;
    @Inject
    protected BeanValidation beanValidation;

    /**
     * Validates UI components by invoking their {@link Validatable#validate()}.
     *
     * @param components components collection
     * @return validation errors
     */
    public ValidationErrors validateUiComponents(Collection<com.haulmont.cuba.gui.components.Component> components) {
        ValidationErrors errors = new ValidationErrors();
        for (com.haulmont.cuba.gui.components.Component component : components) {
            if (component instanceof Validatable) {
                Validatable validatable = (Validatable) component;
                if (validatable.isValidateOnCommit()) {
                    try {
                        validatable.validate();
                    } catch (ValidationException e) {
                        Logger log = LoggerFactory.getLogger(Screen.class);

                        if (log.isTraceEnabled()) {
                            log.trace("Validation failed", e);
                        } else if (log.isDebugEnabled()) {
                            log.debug("Validation failed: " + e);
                        }

                        ComponentsHelper.fillErrorMessages(validatable, e, errors);
                    }
                }
            }
        }
        return errors;
    }

    /**
     * Show validation alert with passed errors and first problem UI component.
     *
     * @param origin screen controller
     * @param errors validation error
     */
    public void showValidationErrors(FrameOwner origin, ValidationErrors errors) {
        checkNotNullArgument(origin);
        checkNotNullArgument(errors);

        if (errors.isEmpty()) {
            return;
        }

        StringBuilder buffer = new StringBuilder();
        for (ValidationErrors.Item error : errors.getAll()) {
            buffer.append(error.description).append("\n");
        }

        ClientConfig clientConfig = configuration.getConfig(ClientConfig.class);

        String validationNotificationType = clientConfig.getValidationNotificationType();
        if (validationNotificationType.endsWith("_HTML")) {
            // HTML validation notification types are not supported
            validationNotificationType = validationNotificationType.replace("_HTML", "");
        }

        Notifications notifications = getScreenContext(origin).getNotifications();

        notifications.builder()
                .withType(Notifications.NotificationType.valueOf(validationNotificationType))
                .withCaption(messages.getMainMessage("validationFail.caption"))
                .withDescription(buffer.toString())
                .show();

        focusProblemComponent(errors);
    }

    protected void focusProblemComponent(ValidationErrors errors) {
        com.haulmont.cuba.gui.components.Component component = null;
        if (!errors.getAll().isEmpty()) {
            component = errors.getAll().get(0).component;
        }
        if (component != null) {
            ComponentsHelper.focusComponent(component);
        }
    }

    /**
     * Validate cross-field BeanValidation rules.
     *
     * @param origin screen controller
     * @param item   item to validate
     * @return validation errors
     */
    public ValidationErrors validateCrossFieldRules(FrameOwner origin, Entity item) {
        ValidationErrors errors = new ValidationErrors();

        Validator validator = beanValidation.getValidator();
        Set<ConstraintViolation<Entity>> violations = validator.validate(item, UiCrossFieldChecks.class);

        violations.stream()
                .filter(violation -> {
                    Path propertyPath = violation.getPropertyPath();

                    Path.Node lastNode = Iterables.getLast(propertyPath);
                    return lastNode.getKind() == ElementKind.BEAN;
                })
                .forEach(violation -> errors.add(violation.getMessage()));

        return errors;
    }

    /**
     * JavaDoc
     *
     * @param origin
     * @param closeAction
     * @return
     */
    public UnsavedChangesDialogResult showUnsavedChangesDialog(FrameOwner origin, CloseAction closeAction) {
        UnsavedChangesDialogResult result = new UnsavedChangesDialogResult();

        Dialogs dialogs = getScreenContext(origin).getDialogs();
        dialogs.createOptionDialog()
                .withCaption(messages.getMainMessage("closeUnsaved.caption"))
                .withMessage(messages.getMainMessage("closeUnsaved"))
                .withType(Dialogs.MessageType.WARNING)
                .withActions(
                        new DialogAction(DialogAction.Type.YES)
                                .withHandler(e -> {

                                    result.yesSelected();
                                }),
                        new DialogAction(DialogAction.Type.NO, Action.Status.PRIMARY)
                                .withHandler(e -> {
                                    Frame frame = UiControllerUtils.getFrame(origin);
                                    ComponentsHelper.focusChildComponent(frame);

                                    result.noSelected();
                                })
                )
                .show();

        return result;
    }

    /**
     * JavaDoc
     *
     * @param origin
     * @param closeAction
     * @return
     */
    public SaveChangesDialogResult showSaveConfirmationDialog(FrameOwner origin, CloseAction closeAction) {
        SaveChangesDialogResult result = new SaveChangesDialogResult();

        Dialogs dialogs = getScreenContext(origin).getDialogs();
        dialogs.createOptionDialog()
                .withCaption(messages.getMainMessage("closeUnsaved.caption"))
                .withMessage(messages.getMainMessage("saveUnsaved"))
                .withActions(
                        new DialogAction(DialogAction.Type.OK, Action.Status.PRIMARY)
                                .withCaption(messages.getMainMessage("closeUnsaved.save"))
                                .withHandler(e -> {

                                    result.commit();
                                }),
                        new BaseAction("discard")
                                .withIcon(icons.get(CubaIcon.DIALOG_CANCEL))
                                .withCaption(messages.getMainMessage("closeUnsaved.discard"))
                                .withHandler(e -> {

                                    result.discard();
                                }),
                        new DialogAction(DialogAction.Type.CANCEL)
                                .withIcon(null)
                                .withHandler(e -> {
                                    Frame frame = UiControllerUtils.getFrame(origin);
                                    ComponentsHelper.focusChildComponent(frame);

                                    result.cancel();
                                })
                )
                .show();

        return result;
    }

    public static class UnsavedChangesDialogResult {
        protected Runnable yesHandler;
        protected Runnable noHandler;

        public UnsavedChangesDialogResult() {
        }

        public UnsavedChangesDialogResult onYes(Runnable onYesHandler) {
            this.yesHandler = onYesHandler;
            return this;
        }

        public UnsavedChangesDialogResult onNo(Runnable onNoHandler) {
            this.noHandler = onNoHandler;
            return this;
        }

        public void yesSelected() {
            if (yesHandler != null) {
                yesHandler.run();
            }
        }

        public void noSelected() {
            if (noHandler != null) {
                noHandler.run();
            }
        }
    }

    public static class SaveChangesDialogResult {
        protected Runnable commitHandler;
        protected Runnable discardHandler;
        protected Runnable cancelHandler;

        public SaveChangesDialogResult() {
        }

        public SaveChangesDialogResult onCommit(Runnable commitHandler) {
            this.commitHandler = commitHandler;
            return this;
        }

        public SaveChangesDialogResult onDiscard(Runnable discardHandler) {
            this.discardHandler = discardHandler;
            return this;
        }

        public SaveChangesDialogResult onCancel(Runnable cancelHandler) {
            this.cancelHandler = cancelHandler;
            return this;
        }

        public void commit() {
            if (commitHandler != null) {
                commitHandler.run();
            }
        }

        public void discard() {
            if (discardHandler != null) {
                discardHandler.run();
            }
        }

        public void cancel() {
            if (cancelHandler != null) {
                cancelHandler.run();
            }
        }
    }
}