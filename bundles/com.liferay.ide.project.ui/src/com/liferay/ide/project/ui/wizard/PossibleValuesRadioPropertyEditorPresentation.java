/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.ide.project.ui.wizard;

import org.eclipse.sapphire.Event;
import org.eclipse.sapphire.Listener;
import org.eclipse.sapphire.modeling.CapitalizationType;
import org.eclipse.sapphire.ui.SapphirePart;
import org.eclipse.sapphire.ui.assist.internal.PropertyEditorAssistDecorator;
import org.eclipse.sapphire.ui.forms.FormComponentPart;
import org.eclipse.sapphire.ui.forms.PropertyEditorPart;
import org.eclipse.sapphire.ui.forms.swt.GridLayoutUtil;
import org.eclipse.sapphire.ui.forms.swt.RadioButtonsGroup;
import org.eclipse.sapphire.ui.forms.swt.SwtPresentation;
import org.eclipse.sapphire.ui.forms.swt.ValuePropertyEditorPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * @author Gregory Amerson
 */
@SuppressWarnings("restriction")
public final class PossibleValuesRadioPropertyEditorPresentation<T> extends ValuePropertyEditorPresentation {

	public PossibleValuesRadioPropertyEditorPresentation(
		FormComponentPart part, SwtPresentation parent, Composite composite) {

		super(part, parent, composite);
	}

	@Override
	protected void createContents(final Composite parent) {
		final PropertyEditorPart part = part();

		final boolean showLabel = part.label() != null;
		final int leftMargin = part.getMarginLeft();

		final Composite composite = createMainComposite(
			parent,
			new CreateMainCompositeDelegate(part) {

				@Override
				public boolean getShowLabel() {
					return false;
				}

				@Override
				public boolean getSpanBothColumns() {
					return true;
				}

			});

		composite.setLayout(GridLayoutUtil.glspacing(GridLayoutUtil.glayout(2, 0, 0), 2, 5));

		PropertyEditorAssistDecorator decorator = createDecorator(composite);

		decorator.addEditorControl(composite);

		Label decoratorControl = decorator.control();

		if (showLabel) {
			GridData gd = GridLayoutUtil.gd();

			decoratorControl.setLayoutData(GridLayoutUtil.gdvalign(gd, SWT.CENTER));

			final Label label = new Label(composite, SWT.WRAP);

			label.setLayoutData(gd);

			final Runnable updateLabelOp = new Runnable() {

				public void run() {
					label.setText(part.label(CapitalizationType.FIRST_WORD_ONLY, true));
				}

			};

			Listener listener = new Listener() {

				@Override
				public void handle(Event event) {
					if (event instanceof SapphirePart.LabelChangedEvent) {
						updateLabelOp.run();
						PossibleValuesRadioPropertyEditorPresentation.this.layout();
					}
				}

			};

			part.attach(listener);

			updateLabelOp.run();

			label.addDisposeListener(
				new DisposeListener() {

					public void widgetDisposed(final DisposeEvent event) {
						part.detach(listener);
					}

				});

			decorator.addEditorControl(label);
		}
		else {
			decoratorControl.setLayoutData(
				GridLayoutUtil.gdvindent(GridLayoutUtil.gdvalign(GridLayoutUtil.gd(), SWT.TOP), 4));
		}

		_control = new RadioButtonsGroup(composite, true);

		if (showLabel) {
			_control.setLayoutData(
				GridLayoutUtil.gdhindent(GridLayoutUtil.gdhspan(GridLayoutUtil.gdhfill(), 2), leftMargin + 20));
		}
		else {
			_control.setLayoutData(GridLayoutUtil.gdhfill());
		}

		binding = new PossibleValuesRadioButtonsGroupBinding<>(this, (RadioButtonsGroup)_control);

		_control.setData(PropertyEditorPart.DATA_BINDING, binding);

		decorator.addEditorControl(_control, true);

		addControl(_control);
	}

	@Override
	protected void handleFocusReceivedEvent() {
		this._control.setFocus();
	}

	private RadioButtonsGroup _control;

}