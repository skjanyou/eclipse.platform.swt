package org.eclipse.swt.widgets;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved
 */

import org.eclipse.swt.internal.win32.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;

/**
 * Instances of this class support the layout of selectable
 * tool bar items.
 * <p>
 * The item children that may be added to instances of this class
 * must be of type <code>ToolItem</code>.
 * </p><p>
 * Note that although this class is a subclass of <code>Composite</code>,
 * it does not make sense to add <code>Control</code> children to it,
 * or set a layout on it.
 * </p><p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>FLAT, WRAP, RIGHT, HORIZONTAL, VERTICAL</dd>
 * <dt><b>Events:</b></dt>
 * <dd>(none)</dd>
 * </dl>
 * <p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 */
public class ToolBar extends Composite {
	ToolItem [] items;
	ImageList imageList, disabledImageList, hotImageList;
	static final int ToolBarProc;
	static final byte [] ToolBarClass = OS.TOOLBARCLASSNAME;
	static {
		WNDCLASSEX lpWndClass = new WNDCLASSEX ();
		lpWndClass.cbSize = WNDCLASSEX.sizeof;
		OS.GetClassInfoEx (0, ToolBarClass, lpWndClass);
		ToolBarProc = lpWndClass.lpfnWndProc;
	}

/**
 * Constructs a new instance of this class given its parent
 * and a style value describing its behavior and appearance.
 * <p>
 * The style value is either one of the style constants defined in
 * class <code>SWT</code> which is applicable to instances of this
 * class, or must be built by <em>bitwise OR</em>'ing together 
 * (that is, using the <code>int</code> "|" operator) two or more
 * of those <code>SWT</code> style constants. The class description
 * for all SWT widget classes should include a comment which
 * describes the style constants which are applicable to the class.
 * </p>
 *
 * @param parent a composite control which will be the parent of the new instance (cannot be null)
 * @param style the style of control to construct
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 *    <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed subclass</li>
 * </ul>
 *
 * @see SWT
 * @see Widget#checkSubclass
 * @see Widget#getStyle
 */
public ToolBar (Composite parent, int style) {
	super (parent, checkStyle (style));
	/*
	* Ensure that either of HORIZONTAL or VERTICAL is set.
	* NOTE: HORIZONTAL and VERTICAL have the same values
	* as H_SCROLL and V_SCROLL so it is necessary to first
	* clear these bits to avoid scroll bars and then reset
	* the bits using the original style supplied by the
	* programmer.
	*/
	this.style = checkBits (style, SWT.HORIZONTAL, SWT.VERTICAL, 0, 0, 0, 0);
}

int callWindowProc (int msg, int wParam, int lParam) {
	if (handle == 0) return 0;
	return OS.CallWindowProc (ToolBarProc, handle, msg, wParam, lParam);
}

static int checkStyle (int style) {
	/*
	* A vertical tool bar cannot wrap because TB_SETROWS
	* fails when the toobar has TBSTYLE_WRAPABLE.
	*/
	if ((style & SWT.VERTICAL) != 0) style &= ~SWT.WRAP;
	
	/*
	* Even though it is legal to create this widget
	* with scroll bars, they serve no useful purpose
	* because they do not automatically scroll the
	* widget's client area.  The fix is to clear
	* the SWT style.
	*/
	return style & ~(SWT.H_SCROLL | SWT.V_SCROLL);
}

protected void checkSubclass () {
	if (!isValidSubclass ()) error (SWT.ERROR_INVALID_SUBCLASS);
}

public Point computeSize (int wHint, int hHint, boolean changed) {
	checkWidget ();
	if (layout != null) {
		return super.computeSize (wHint, hHint, changed);
	}
	int width = 0, height = 0;
	RECT oldRect = new RECT ();
	OS.GetWindowRect (handle, oldRect);
	int oldWidth = oldRect.right - oldRect.left;
	int oldHeight = oldRect.bottom - oldRect.top;
	int newWidth = wHint, newHeight = hHint;
	if (newWidth == SWT.DEFAULT) newWidth = 0x3FFF;
	if (newHeight == SWT.DEFAULT) newHeight = 0x3FFF;
	boolean redraw = drawCount == 0 && OS.IsWindowVisible (handle);
	if (redraw) {
		OS.UpdateWindow (handle);
		/*
		* This line is intentionally commented.
		*/
//		OS.SendMessage (handle, OS.WM_SETREDRAW, 0, 0);
		OS.DefWindowProc (handle, OS.WM_SETREDRAW, 0, 0);
	}
	int flags = OS.SWP_NOACTIVATE | OS.SWP_NOMOVE | OS.SWP_NOREDRAW | OS.SWP_NOZORDER;
	OS.SetWindowPos (handle, 0, 0, 0, newWidth, newHeight, flags);
	int count = OS.SendMessage (handle, OS.TB_BUTTONCOUNT, 0, 0);
	if (count != 0) {
		RECT rect = new RECT ();
		OS.SendMessage (handle, OS.TB_GETITEMRECT, count - 1, rect);
		width = Math.max (width, rect.right);
		height = Math.max (height, rect.bottom);
	}
	OS.SetWindowPos (handle, 0, 0, 0, oldWidth, oldHeight, flags);
	if (redraw) {
		/*
		* This line is intentionally commented.
		*/
//		OS.SendMessage (handle, OS.WM_SETREDRAW, 1, 0);
		OS.DefWindowProc (handle, OS.WM_SETREDRAW, 1, 0);
		OS.RedrawWindow (handle, null, 0, OS.RDW_INVALIDATE | OS.RDW_UPDATENOW);
	}
	
	/*
	* From the Windows SDK for TB_SETBUTTONSIZE:
	*
	*   "If an application does not explicitly
	*	set the button size, the size defaults
	*	to 24 by 22 pixels". 
	*/
	if (width == 0) width = 24;
	if (height == 0) height = 22;
	if (wHint != SWT.DEFAULT) width = wHint;
	if (hHint != SWT.DEFAULT) height = hHint;
	Rectangle trim = computeTrim (0, 0, width, height);
	width = trim.width;  height = trim.height;
	return new Point (width, height);
}


public Rectangle computeTrim (int x, int y, int width, int height) {
	checkWidget ();
	Rectangle trim = super. computeTrim (x, y, width, height);
	int bits = OS.GetWindowLong (handle, OS.GWL_STYLE);
	if ((bits & OS.CCS_NODIVIDER) == 0) trim.height += 2;
	return trim;
}

void createHandle () {
	super.createHandle ();
	state &= ~CANVAS;

	/*
	* Feature in Windows.  When the control is created,
	* it does not use the default system font.  A new HFONT
	* is created and destroyed when the control is destroyed.
	* This means that a program that queries the font from
	* this control, uses the font in another control and then
	* destroys this control will have the font unexpectedly
	* destroyed in the other control.  The fix is to assign
	* the font ourselves each time the control is created.
	* The control will not destroy a font that it did not
	* create.
	*/
	int hFont = OS.GetStockObject (OS.SYSTEM_FONT);
	OS.SendMessage (handle, OS.WM_SETFONT, hFont, 0);

	/* Set the button struct, bitmap and button sizes */
	OS.SendMessage (handle, OS.TB_BUTTONSTRUCTSIZE, TBBUTTON.sizeof, 0);
	OS.SendMessage (handle, OS.TB_SETBITMAPSIZE, 0, 0);
	OS.SendMessage (handle, OS.TB_SETBUTTONSIZE, 0, 0);

	/* Set the extended style bits */
	int bits = OS.TBSTYLE_EX_DRAWDDARROWS;
	OS.SendMessage (handle, OS.TB_SETEXTENDEDSTYLE, 0, bits);
}

void createItem (ToolItem item, int index) {
	int count = OS.SendMessage (handle, OS.TB_BUTTONCOUNT, 0, 0);
	if (!(0 <= index && index <= count)) error (SWT.ERROR_INVALID_RANGE);
	int id = 0;
	while (id < items.length && items [id] != null) id++;
	if (id == items.length) {
		ToolItem [] newItems = new ToolItem [items.length + 4];
		System.arraycopy (items, 0, newItems, 0, items.length);
		items = newItems;
	}
	int bits = item.widgetStyle ();
	TBBUTTON lpButton = new TBBUTTON ();
	lpButton.idCommand = id;
	lpButton.fsStyle = (byte) bits;
	lpButton.fsState = (byte) OS.TBSTATE_ENABLED;
	
	/*
	* Bug in Windows.  Despite the fact that the image list
	* index has never been set for the item, Windows always
	* assumes that the image index for the item is valid.
	* When an item is inserted, the image index is zero.
	* Therefore, when the first image is inserted and is
	* assigned image index zero, every item draws with this
	* image.  The fix is to set the image index to none
	* when the item is created.  This is not necessary in
	* the case when the item has the BTNS_SEP style because
	* separators cannot show images.
	*/
	if ((bits & OS.BTNS_SEP) == 0) lpButton.iBitmap = OS.I_IMAGENONE;
	if (OS.SendMessage (handle, OS.TB_INSERTBUTTON, index, lpButton) == 0) {
		error (SWT.ERROR_ITEM_NOT_ADDED);
	}
	items [item.id = id] = item;
	if ((style & SWT.VERTICAL) != 0) {
		OS.SendMessage (handle, OS.TB_SETROWS, count+1, 0);
	}
}

void createWidget () {
	super.createWidget ();
	items = new ToolItem [4];
}

void destroyItem (ToolItem item) {
	TBBUTTONINFO info = new TBBUTTONINFO ();
	info.cbSize = TBBUTTONINFO.sizeof;
	info.dwMask = OS.TBIF_IMAGE | OS.TBIF_STYLE;
	int index = OS.SendMessage (handle, OS.TB_GETBUTTONINFO, item.id, info);
	/*
	* Feature in Windows.  For some reason, a tool item that has
	* the style BTNS_SEP does not return I_IMAGENONE when queried
	* for an image index, despite the fact that no attempt has been
	* made to assign an image to the item.  As a result, operations
	* on an image list that use the wrong index cause random results.	
	* The fix is to ensure that the tool item is not a separator
	* before using the image index.  Since separators cannot have
	* an image and one is never assigned, this is not a problem.
	*/
	if ((info.fsStyle & OS.BTNS_SEP) == 0 && info.iImage != OS.I_IMAGENONE) {
		if (imageList != null) imageList.put (info.iImage, null);
		if (hotImageList != null) hotImageList.put (info.iImage, null);
		if (disabledImageList != null) disabledImageList.put (info.iImage, null);
	}
	int result = OS.SendMessage (handle, OS.TB_DELETEBUTTON, index, 0);
	items [item.id] = null;
	item.id = -1;
	int count = OS.SendMessage (handle, OS.TB_BUTTONCOUNT, 0, 0);
	if (count == 0) {
		if (imageList != null) {
			OS.SendMessage (handle, OS.TB_SETIMAGELIST, 0, 0);
			imageList.dispose ();
		}
		if (hotImageList != null) {
			OS.SendMessage (handle, OS.TB_SETHOTIMAGELIST, 0, 0);
			hotImageList.dispose ();
		}
		if (disabledImageList != null) {
			OS.SendMessage (handle, OS.TB_SETDISABLEDIMAGELIST, 0, 0);
			disabledImageList.dispose ();
		}
		imageList = hotImageList = disabledImageList = null;
		items = new ToolItem [4];
	}
	if ((style & SWT.VERTICAL) != 0) {
		OS.SendMessage (handle, OS.TB_SETROWS, count-1, 0);
	}
}

ImageList getDisabledImageList () {
	return disabledImageList;
}

ImageList getHotImageList () {
	return hotImageList;
}

ImageList getImageList () {
	return imageList;
}

/**
 * Returns the item at the given, zero-relative index in the
 * receiver. Throws an exception if the index is out of range.
 *
 * @param index the index of the item to return
 * @return the item at the given index
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public ToolItem getItem (int index) {
	checkWidget ();
	int count = OS.SendMessage (handle, OS.TB_BUTTONCOUNT, 0, 0);
	if (!(0 <= index && index < count)) error (SWT.ERROR_INVALID_RANGE);	
	TBBUTTON lpButton = new TBBUTTON ();
	int result = OS.SendMessage (handle, OS.TB_GETBUTTON, index, lpButton);
	if (result == 0) error (SWT.ERROR_CANNOT_GET_ITEM);
	return items [lpButton.idCommand];
}

/**
 * Returns the item at the given point in the receiver
 * or null if no such item exists. The point is in the
 * coordinate system of the receiver.
 *
 * @param point the point used to locate the item
 * @return the item at the given point
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public ToolItem getItem (Point point) {
	checkWidget ();
	if (point == null) error (SWT.ERROR_NULL_ARGUMENT);
	ToolItem [] items = getItems ();
	for (int i=0; i<items.length; i++) {
		Rectangle rect = items [i].getBounds ();
		if (rect.contains (point)) return items [i];
	}
	return null;
}

/**
 * Returns the number of items contained in the receiver.
 *
 * @return the number of items
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getItemCount () {
	checkWidget ();
	return OS.SendMessage (handle, OS.TB_BUTTONCOUNT, 0, 0);
}

/**
 * Returns an array of <code>TabItem</code>s which are the items
 * in the receiver. 
 * <p>
 * Note: This is not the actual structure used by the receiver
 * to maintain its list of items, so modifying the array will
 * not affect the receiver. 
 * </p>
 *
 * @return the items in the receiver
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public ToolItem [] getItems () {
	checkWidget ();
	int count = OS.SendMessage (handle, OS.TB_BUTTONCOUNT, 0, 0);
	TBBUTTON lpButton = new TBBUTTON ();
	ToolItem [] result = new ToolItem [count];
	for (int i=0; i<count; i++) {
		int code = OS.SendMessage (handle, OS.TB_GETBUTTON, i, lpButton);
		result [i] = items [lpButton.idCommand];
	}
	return result;
}

/**
 * Returns the number of rows in the receiver. When
 * the receiver has the <code>WRAP</code> style, the
 * number of rows can be greater than one.  Otherwise,
 * the number of rows is always one.
 *
 * @return the number of items
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getRowCount () {
	checkWidget ();
	return OS.SendMessage (handle, OS.TB_GETROWS, 0, 0);
}

/**
 * Searches the receiver's list starting at the first item
 * (index 0) until an item is found that is equal to the 
 * argument, and returns the index of that item. If no item
 * is found, returns -1.
 *
 * @param item the search item
 * @return the index of the item
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the string is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int indexOf (ToolItem item) {
	checkWidget ();
	if (item == null) error (SWT.ERROR_NULL_ARGUMENT);
	return OS.SendMessage (handle, OS.TB_COMMANDTOINDEX, item.id, 0);
}

void releaseWidget () {
	for (int i=0; i<items.length; i++) {
		ToolItem item = items [i];
		if (item != null && !item.isDisposed ()) {
			item.releaseWidget ();
		}
	}
	items = null;
	super.releaseWidget ();
	if (imageList != null) {
		OS.SendMessage (handle, OS.TB_SETIMAGELIST, 0, 0);
		imageList.dispose ();
	}
	if (hotImageList != null) {
		OS.SendMessage (handle, OS.TB_SETHOTIMAGELIST, 0, 0);
		hotImageList.dispose ();
	}
	if (disabledImageList != null) {
		OS.SendMessage (handle, OS.TB_SETDISABLEDIMAGELIST, 0, 0);
		disabledImageList.dispose ();
	}
	imageList = hotImageList = disabledImageList = null;
}

void setDefaultFont () {
	super.setDefaultFont ();
	OS.SendMessage (handle, OS.TB_SETBITMAPSIZE, 0, 0);
	OS.SendMessage (handle, OS.TB_SETBUTTONSIZE, 0, 0);
}

void setDisabledImageList (ImageList imageList) {
	if (disabledImageList == imageList) return;
	int hImageList = 0;
	if ((disabledImageList = imageList) != null) {
		hImageList = disabledImageList.getHandle ();
		disabledImageList.setBackground (getBackgroundPixel ());
	}
	OS.SendMessage (handle, OS.TB_SETDISABLEDIMAGELIST, 0, hImageList);
}


public boolean setFocus () {
	return false;
}

void setHotImageList (ImageList imageList) {
	if (hotImageList == imageList) return;
	int hImageList = 0;
	if ((hotImageList = imageList) != null) {
		hImageList = hotImageList.getHandle ();
		hotImageList.setBackground (getBackgroundPixel ());
	}
	OS.SendMessage (handle, OS.TB_SETHOTIMAGELIST, 0, hImageList);
}

void setImageList (ImageList imageList) {
	if (this.imageList == imageList) return;
	int hImageList = 0;
	if ((this.imageList = imageList) != null) {
		hImageList = imageList.getHandle ();
		imageList.setBackground (getBackgroundPixel ());
	}
	OS.SendMessage (handle, OS.TB_SETIMAGELIST, 0, hImageList);
}

int toolTipHandle () {
	return OS.SendMessage (handle, OS.TB_GETTOOLTIPS, 0, 0);
}

String toolTipText (NMTTDISPINFO hdr) {
	if ((hdr.uFlags & OS.TTF_IDISHWND) != 0) {
		return null;
	}
	int index = hdr.idFrom;
	int hwndToolTip = toolTipHandle ();
	if (hwndToolTip == hdr.hwndFrom) {
		if ((0 <= index) && (index < items.length)) {
			ToolItem item = items [index];
			if (item != null) return item.toolTipText;
		}
	}
	return super.toolTipText (hdr);
}

int widgetStyle () {
	int bits = super.widgetStyle () | OS.CCS_NODIVIDER | OS.CCS_NORESIZE | OS.TBSTYLE_TOOLTIPS;
	if ((style & SWT.WRAP) != 0) bits |= OS.TBSTYLE_WRAPABLE;
	if ((style & SWT.FLAT) != 0) bits |= OS.TBSTYLE_FLAT;
	if ((style & SWT.RIGHT) != 0) bits |= OS.TBSTYLE_LIST;
	return bits;
}

byte [] windowClass () {
	return ToolBarClass;
}

int windowProc () {
	return ToolBarProc;
}

LRESULT WM_COMMAND (int wParam, int lParam) {
	/*
	* Feature in Windows.  When the toolbar window
	* proc processes WM_COMMAND, it forwards this
	* message to the parent.  This is done so that
	* children of the toolbar that send WM_COMMAND
	* messages to their parents will notify not only
	* the toolbar but also the parent of the toolbar,
	* which is typically the application window and
	* the window that is looking for this message.
	* If the toolbar did not do this, applications
	* would have to subclass the toolbar window to
	* see WM_COMMAND messages. Because the toolbar
	* window is subclassed, the WM_COMMAND message
	* is delivered twice.  The fix is to avoid
	* calling the toolbar window proc.
	*/
	LRESULT result = super.WM_COMMAND (wParam, lParam);
	if (result != null) return result;
	return LRESULT.ZERO;
}

LRESULT WM_NOTIFY (int wParam, int lParam) {
	/*
	* Bug in Windows NT.  For some reason, Windows NT requests a
	* UNICODE tool tip string instead of a DBCS string by sending
	* TTN_GETDISPINFOW instead of TTN_GETDISPINFOA.  This is not
	* correct because the control is created as a DBCS control and
	* expects to process TTN_GETDISPINFOA.  TTN_GETDISPINFOA is
	* never sent on NT.  The fix is to handle TTN_GETDISPINFOW and
	* give the control a UNICODE string.
	*/
	if (IsWinNT) {
		NMHDR hdr = new NMHDR ();
		OS.MoveMemory (hdr, lParam, NMHDR.sizeof);
		if (hdr.code == OS.TTN_GETDISPINFOW) {
			NMTTDISPINFO lpnmtdi = new NMTTDISPINFO ();
			OS.MoveMemory (lpnmtdi, lParam, NMTTDISPINFO.sizeof);
			String string = null;
			int index = hdr.idFrom;
			if (0 <= index && index < items.length) {
				ToolItem item = items [index];
				if (item != null) string = item.toolTipText;
			}
			if (string != null && string.length () != 0) {
				int length = string.length ();
				char [] buffer = new char [length + 1];
				string.getChars (0, length, buffer, 0);
				getShell ().setToolTipText (lpnmtdi, buffer);
				OS.MoveMemory (lParam, lpnmtdi, NMTTDISPINFO.sizeof);
			}
			return LRESULT.ZERO;
		}
	}
	return super.WM_NOTIFY (wParam, lParam);
}

LRESULT WM_SIZE (int wParam, int lParam) {
	LRESULT result = super.WM_SIZE (wParam, lParam);
	/*
	* It is possible (but unlikely), that application
	* code could have disposed the widget in the resize
	* event.  If this happens, end the processing of the
	* Windows message by returning the result of the
	* WM_SIZE message.
	*/
	if (isDisposed ()) return result;
	for (int i=0; i<items.length; i++) {
		ToolItem item = items [i];
		if (item != null) {
			Control control = item.control;
			if (control != null && !control.isDisposed ()) {
				Rectangle rect = item.getBounds ();
				control.setLocation (rect.x, rect.y);
			}
		}
	}
	return result;
}

LRESULT WM_SYSCOLORCHANGE (int wParam, int lParam) {
	LRESULT result = super.WM_SYSCOLORCHANGE (wParam, lParam);
	if (result != null) return result;
	if (imageList != null && background == -1) {
		imageList.setBackground (defaultBackground ());
		int hImageList = imageList.getHandle ();
		OS.SendMessage (handle, OS.TB_SETIMAGELIST, 0, hImageList);
	}
	if (hotImageList != null && background == -1) {
		hotImageList.setBackground (defaultBackground ());
		int hImageList = hotImageList.getHandle ();
		OS.SendMessage (handle, OS.TB_SETHOTIMAGELIST, 0, hImageList);
	}
	if (disabledImageList != null && background == -1) {
		disabledImageList.setBackground (defaultBackground ());
		int hImageList = disabledImageList.getHandle ();
		OS.SendMessage (handle, OS.TB_SETDISABLEDIMAGELIST, 0, hImageList);
	}
	return result;
}

LRESULT wmCommandChild (int wParam, int lParam) {
	ToolItem child = items [wParam & 0xFFFF];
	if (child == null) return null;
	return child.wmCommandChild (wParam, lParam);
}

LRESULT wmNotifyChild (int wParam, int lParam) {
	NMHDR hdr = new NMHDR ();
	OS.MoveMemory (hdr, lParam, NMHDR.sizeof);
	switch (hdr.code) {
		case OS.TBN_DROPDOWN:
			NMTOOLBAR lpnmtb = new NMTOOLBAR ();
			OS.MoveMemory (lpnmtb, lParam, NMTOOLBAR.sizeof);
			ToolItem child = items [lpnmtb.iItem];
			if (child != null) {
				Event event = new Event ();
				event.detail = SWT.ARROW;
				child.postEvent (SWT.Selection, event);
				return null;
			}
			break;
	}
	return super.wmNotifyChild (wParam, lParam);
}

}
