package kr.poturns.blink.external;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import kr.poturns.blink.R;
import android.annotation.SuppressLint;
import android.content.ClipData;

/**
 * 기존 존재하는 ViewGroup에 원의 형태로 ChildView를 추가해주고, <br>
 * 핵심 View와 기타 ChildView 와의 Drag and Drop event를 받기 쉽게 만들어주는 도우미 Class <br>
 * <br>
 * * 현재 지원하는 {@link android.view.ViewGroup}은 {@link android.widget.FrameLayout}에
 * 한정된다. <br>
 * <br>
 * <b><사용 예시></b><br>
 * 
 * <pre class="prettyprint">
 * android.widget.FrameLayout someFrameLayout = (android.widget.FrameLayout) someParentView
 * 		.findViewById(R.id.someId);
 * 
 * int size = 11;
 * List&lt;Integer&gt; list = new ArrayList&lt;Integer&gt;(size);
 * for (int i = 0; i &lt; size; i++) {
 * 	list.add(i);
 * }
 * android.widget.TextView centerView = new android.widget.TextView(someContext);
 * centerView.setText(&quot;center&quot;);
 * 
 * CircularViewHelper circularAdapter = new CircularViewHelper(someFrameLayout,
 * 		centerView) {
 * 	// childView의 형태를 정의
 * 	protected android.view.View getView(int position, Object object){
 * 		return android.view.View.inflate(....);
 * 	}
 * };
 * 
 * circularAdapter.drawChildViews(list);
 * 
 * </pre>
 * 
 * 
 * @see android.view.ViewGroup
 * @see android.widget.FrameLayout
 * 
 * 
 */
class CircularViewHelper {
	private android.view.ViewGroup mViewGroup;
	private List<android.view.View> mChildViewList;
	private android.view.View mCenterView;
	private android.content.Context mContext;
	private int mCenterViewId;
	/** ChildView의 지름 */
	private int mViewSize;
	/** CenterView 와 ChildView와의 거리 */
	private int mChildViewDistance;
	/** ChildView 개수 */
	private int mObjectSize;
	private android.widget.FrameLayout.LayoutParams mLayoutParams;
	private static final int CENTER_VIEW_POSITION = -2;
	private static final int VIEW_NO_ID = -99;

	/**
	 * CircularViewHelper instance를 생성한다.
	 * 
	 * @param viewGroup
	 *            원형 View가 추가될 ViewGroup
	 */
	public CircularViewHelper(android.view.ViewGroup viewGroup) {
		this(viewGroup, VIEW_NO_ID);
	}

	/**
	 * CircularViewHelper instance를 생성한다.
	 * 
	 * @param viewGroup
	 *            원형 View가 추가될 ViewGroup
	 * @param centerViewId
	 *            원 가운데 배치된 View의 id
	 */
	public CircularViewHelper(android.view.ViewGroup viewGroup, int centerViewId) {
		if (!checkVaildViewGroup(viewGroup))
			throw new RuntimeException(
					"CircularViewHelper only accept FrameLayout");
		this.mViewGroup = viewGroup;
		if (centerViewId != VIEW_NO_ID)
			mCenterView = viewGroup.findViewById(centerViewId);
		this.mCenterViewId = centerViewId;
		mContext = mViewGroup.getContext();
		mChildViewList = new ArrayList<android.view.View>();
	}

	/**
	 * CircularViewHelper instance를 생성한다.
	 * 
	 * @param viewGroup
	 *            원형 View가 추가될 ViewGroup
	 * @param centerView
	 *            원 가운데 배치된 View
	 */
	public CircularViewHelper(android.view.ViewGroup viewGroup,
			android.view.View centerView) {
		if (!checkVaildViewGroup(viewGroup))
			throw new RuntimeException(
					"CircularViewHelper only accept FrameLayout");
		this.mViewGroup = viewGroup;
		mContext = mViewGroup.getContext();
		mChildViewList = new ArrayList<android.view.View>();
		mCenterView = centerView;
		setCenterViewIdIfNotExist();
	}

	/* 현재 지원하는 ViewGroup에 맞는 ViewGroup instance 인지 검사한다 */
	private boolean checkVaildViewGroup(android.view.ViewGroup viewGroup) {
		return viewGroup instanceof android.widget.FrameLayout;
	}

	/** 중앙에 배치된 View를 얻는다. */
	public final android.view.View getCenterView() {
		return mCenterView;
	}

	/** 중앙에 배치될 View를 설정한다. */
	public final void setCenterView(android.view.View centerView) {
		if (mCenterView != null)
			mViewGroup.removeView(mCenterView);
		this.mCenterView = centerView;
		setCenterViewIdIfNotExist();
	}

	/**
	 * 중앙에 배치될 View를 설정한다.<br>
	 * <br>
	 * 생성되는 View를 커스터마이징하려면
	 * {@link CircularViewHelper#createCenterView(android.content.Context, Object)}
	 * 를 재정의 한다.
	 * 
	 * @param obj
	 *            생성하려는 View와 관련된 객체
	 */
	public final void setCenterViewFromObject(Object obj) {
		android.view.View center = createCenterView(mContext, obj);
		center.setTag(obj);
		setCenterView(center);
	}

	/**
	 * CenterView를 생성한다.
	 * 
	 * @param context
	 *            {@link CircularViewHelper}의 Context
	 * @param object
	 *            생성하려는 View와 관련된 객체
	 * @return CircularViewHelper에 가운데에 위치하려는 View
	 * @see {@link CircularViewHelper#setCenterViewFromObject(Object)}
	 * */
	protected android.view.View createCenterView(
			android.content.Context context, Object object) {
		return android.view.View.inflate(context,
				R.layout.res_blink_view_circular, null);
	}

	/* 중앙에 배치된 View의 Id가 없는 경우 설정한다. */
	private void setCenterViewIdIfNotExist() {
		if (mCenterView == null) {
			mCenterViewId = VIEW_NO_ID;
			return;
		}
		mCenterViewId = mCenterView.getId();
		if (mCenterViewId == android.view.View.NO_ID
				|| mCenterViewId == VIEW_NO_ID) {
			mCenterViewId = android.view.View.generateViewId();
			mCenterView.setId(mCenterViewId);
		}
	}

	/* 모든 Child View를 ViewGroup으로 부터 떼어낸다. */
	private void removeChildViewAll() {
		if (mChildViewList != null) {
			int size = mChildViewList.size();
			for (int i = 0; i < size; i++) {
				mViewGroup.removeView(mChildViewList.get(i));
			}
		}
	}

	/**
	 * 원형 View를 구성하는 Data가 변경되었을때,<br>
	 * 원형으로 배치할 View의 크기 등등의 변수(Spec)를 다시 측정한다.
	 */
	protected void measuringSpec(int newSize) {
		final int screenSize = Math.min(mContext.getResources()
				.getDisplayMetrics().widthPixels, mContext.getResources()
				.getDisplayMetrics().heightPixels);
		int actionBarSize;
		if (PrivateUtil.isScreenSizeSmall(mContext)) {
			actionBarSize = 20;
		} else {
			final android.content.res.TypedArray styledAttributes = mContext
					.getTheme().obtainStyledAttributes(
							new int[] { android.R.attr.actionBarSize });
			actionBarSize = (int) styledAttributes.getDimension(0, 0);
			styledAttributes.recycle();
		}

		mViewSize = (screenSize - actionBarSize) / Math.max(newSize, 8) * 2;
		mChildViewDistance = (screenSize - mViewSize - actionBarSize * 2) / 2;
		mLayoutParams = new android.widget.FrameLayout.LayoutParams(mViewSize,
				mViewSize);
		mLayoutParams.gravity = android.view.Gravity.CENTER;
	}

	/** 중앙의 View를 현재 Spec에 맞게 다시 그리고, 필요하다면 주어진 ViewGroup에 추가한다 */
	private void setCenterViewSpec() {
		if (mCenterView == null)
			return;
		ViewInfoTag tag = new ViewInfoTag();
		tag.mIsDrag = false;
		tag.mViewPosition = CENTER_VIEW_POSITION;
		tag.mViewId = mCenterViewId;
		Object objTag = mCenterView.getTag();
		if (objTag instanceof ViewInfoTag) {
			tag.mTag = ((ViewInfoTag) objTag).mTag;
		} else {
			tag.mTag = objTag;
		}

		mCenterView.setTag(tag);
		mCenterView.setOnDragListener(mOnDragListener);
		mCenterView.setLayoutParams(mLayoutParams);

		if (!mViewGroup.equals(mCenterView.getParent()))
			mViewGroup.addView(mCenterView);
	}

	/** ViewGroup에 ChildView를 배치한다. */
	public final void drawCircularView(Collection<? extends Object> datas) {
		removeChildViewAll();
		mChildViewList.clear();
		if (datas == null) {
			mObjectSize = 0;
			measuringSpec(mObjectSize);
			setCenterViewSpec();
			return;
		}
		mObjectSize = datas.size();
		measuringSpec(mObjectSize);
		setCenterViewSpec();
		android.view.View child;
		int i = 0;
		for (Object obj : datas) {
			child = getView(mContext, i, obj);
			child.setTag(obj);
			setChildViewInfo(child, i, mObjectSize);
			mViewGroup.addView(child);
			mChildViewList.add(child);
			i++;
		}
	}

	/**
	 * 원형으로 배치할 childView를 생성한다. <br>
	 * ChildView를 임의의 형태로 inflate하려면 재정의 할 수 있다.
	 * 
	 * @param context
	 *            - Circular View를 구성하는 ViewGroup의 Context
	 * @param position
	 *            - {@link CircularViewHelper#drawCircularView(Collection)}에서
	 *            인자로 넘겨준 {@link Collection}의 {@link Iterator}를 통해 얻어낸, <br>
	 *            구성되려는 ChildView의 위치
	 * @param object
	 *            - {@link Collection}의 data
	 */
	protected android.view.View getView(android.content.Context context,
			int position, Object object) {
		return android.view.View.inflate(context,
				R.layout.res_blink_view_circular, null);
	}

	/** 현재 배치된 Child View의 개수를 얻는다. */
	public int getSize() {
		return mObjectSize;
	}

	/* 측정된 spec에 따라 Child View의 배치를 설정한다. */
	private void setChildViewInfo(android.view.View childView, int postion,
			int size) {
		ViewInfoTag tag = new ViewInfoTag();
		tag.mIsDrag = false;
		tag.mViewId = postion;
		tag.mViewPosition = postion;
		tag.mTag = childView.getTag();
		childView.setTag(tag);
		childView.setLayoutParams(mLayoutParams);
		float angleDeg = postion * 360.0f / size - 90.0f;
		float angleRad = (float) (angleDeg * Math.PI / 180.0f);
		childView.setTranslationX(mChildViewDistance
				* (float) Math.cos(angleRad));
		childView.setTranslationY(mChildViewDistance
				* (float) Math.sin(angleRad));
		// tv.setRotation(angleDeg + 90.0f);
		childView.setOnTouchListener(mOnTouchListener);
		childView.setOnDragListener(mOnDragListener);
	}

	/** */
	public final List<android.view.View> getChildViews() {
		return mChildViewList;
	}

	/**/
	private android.view.View.OnTouchListener mOnTouchListener = new android.view.View.OnTouchListener() {
		@SuppressLint("ClickableViewAccessibility")
		@Override
		public boolean onTouch(final android.view.View v,
				android.view.MotionEvent event) {
			if (v.getAlpha() == 1.0f) {
				ViewInfoTag tag = (ViewInfoTag) v.getTag();
				tag.mIsDrag = true;
				ClipData data = ClipData.newPlainText("", tag.toString());
				v.startDrag(data, new android.view.View.DragShadowBuilder(v),
						v, 0);
			}
			return true;
		}
	};
	/* Touch 이벤트에서 Click 이벤트를 호출할 때 사용하는 Handler */
	private static final android.os.Handler sHandler = new android.os.Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case ACTION_CLICK:
				android.view.View v = (android.view.View) msg.obj;
				v.performClick();
				break;
			default:
				break;
			}
			msg.recycle();
		}
	};
	/** Touch 이벤트에서 Handler를 통해 Click 이벤트를 호출할 때 사용하는 상수 */
	private static final int ACTION_CLICK = 888;
	/* Drag 이벤트를 처리하는 리스너 */
	private final android.view.View.OnDragListener mOnDragListener = new android.view.View.OnDragListener() {

		@Override
		public boolean onDrag(android.view.View v, android.view.DragEvent event) {
			final int action = event.getAction();
			ViewInfoTag tag = (ViewInfoTag) v.getTag();

			switch (action) {
			case android.view.DragEvent.ACTION_DRAG_STARTED:
				return true;
			case android.view.DragEvent.ACTION_DRAG_ENTERED:
				// Drag를 시작하는 View에게만 이벤트 전달
				if (tag.mIsDrag && mDragAndDropListener != null)
					mDragAndDropListener.onStartDrag(v, mCenterView);
				return true;
			case android.view.DragEvent.ACTION_DRAG_LOCATION:
				return true;
			case android.view.DragEvent.ACTION_DRAG_EXITED:
				// Drag가 해당 View의 범위를 벗어나면 Click 이벤트를 발생시키지 않는다.
				sHandler.removeMessages(ACTION_CLICK);
				return true;
			case android.view.DragEvent.ACTION_DROP:
				return checkDropEvent(v, tag, event);
			case android.view.DragEvent.ACTION_DRAG_ENDED:
				// Drag를 시작한 View에게만 이벤트 전달
				if (tag.mIsDrag) {
					// Center View로 Drop된 것이 확실한 경우
					// Click 이벤트를 무시하고, Drop 이벤트를 전달한다.
					if (event.getResult() && tag.mDropViewId == mCenterViewId
							&& mDragAndDropListener != null) {
						sHandler.removeMessages(ACTION_CLICK);
						mDragAndDropListener.onDrop(v);
					}
					// View의 Drag 이벤트 정보 초기화
					tag.mDropViewId = android.view.View.NO_ID;
					tag.mIsDrag = false;
					if (mDragAndDropListener != null)
						mDragAndDropListener.onDropEnd(v, mCenterView);
				}
				return true;
			default:
				return false;
			}
		}

		/* Drop 이벤트를 검사한다. */
		private boolean checkDropEvent(android.view.View v, ViewInfoTag tag,
				android.view.DragEvent event) {
			// View가 Drop되었을 때, Drop된 장소가 해당 View인 경우
			// 즉 Click 이벤트를 실행하기 원하는 경우
			// Click 이벤트를 실행하고, Drag 이벤트는 무시한다.
			if (tag.mIsDrag && !sHandler.hasMessages(ACTION_CLICK)) {
				android.os.Message m = android.os.Message.obtain();
				m.what = ACTION_CLICK;
				m.obj = v;
				sHandler.sendMessageDelayed(m, 100);
				return false;
			}
			// Center View로 Drop된 경우
			if (tag.mViewPosition == CENTER_VIEW_POSITION) {
				ViewInfoTag draggerTag = (ViewInfoTag) ((android.view.View) event
						.getLocalState()).getTag();
				draggerTag.mDropViewId = tag.mViewId;
				return true;
			} else
				return false;
		}
	};

	/**
	 * CircularView에 속한 View의 Tag를 얻는다.
	 * 
	 * @param postion
	 *            얻어오려는 View의 위치
	 * @throws ClassCastException
	 * @see View#getTag()
	 */
	public final Object getViewTag(int position) {
		return ((ViewInfoTag) mChildViewList.get(position).getTag()).mTag;
	}

	/**
	 * CircularView에 속한 View의 Tag를 얻는다.
	 * 
	 * @param Tag
	 *            얻어오려는 View
	 * @throws ClassCastException
	 * @see View#getTag()
	 */
	public final Object getViewTag(android.view.View childView) {
		return ((ViewInfoTag) childView.getTag()).mTag;
	}

	/*
	 * CircularViewHelper 클래스 내부에서 터치 이벤트 등등 각종 이벤트를 처리하기 위해 View 객체에 저장해 놓는 정보
	 * 클래스
	 */
	private static class ViewInfoTag {
		public boolean mIsDrag;
		public int mViewId;
		public int mDropViewId = android.view.View.NO_ID;
		public int mViewPosition;
		public Object mTag;
	}

	/* Drag & Drop 이벤트를 처리할 리스너 */
	private OnDragAndDropListener mDragAndDropListener;

	/** {@link OnDragAndDropListener}를 설정한다. */
	public final void setOnDragAndDropListener(OnDragAndDropListener l) {
		mDragAndDropListener = l;
	}

	/** Child View가 Center View로의 Drag & Drop 되는 이벤트를 처리할 리스너 */
	public static interface OnDragAndDropListener {
		/**
		 * 터치한 View의 Drag가 시작되었을 때, 호출된다.
		 * 
		 * @param view
		 *            Drag를 시작한 View
		 * @param center
		 *            중앙의 View
		 */
		public void onStartDrag(android.view.View view, android.view.View center);

		/**
		 * Drag하던 View가 중앙의 View에 Drop되었을 때, 호출 됨
		 * 
		 * @param view
		 *            Drag하던 뷰
		 */
		public void onDrop(android.view.View view);

		/**
		 * 터치한 View의 Drag가 종료되었을 때, 호출된다.
		 * 
		 * @param view
		 *            Drag를 시작한 View
		 * @param center
		 *            중앙의 View
		 */
		public void onDropEnd(android.view.View view, android.view.View center);
	}
}