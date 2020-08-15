package com.github.wangji92.arthas.plugin.action.arthas;

import com.github.wangji92.arthas.plugin.utils.ClipboardUtils;
import com.github.wangji92.arthas.plugin.utils.NotifyUtils;
import com.github.wangji92.arthas.plugin.utils.OgnlPsUtils;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

/**
 * ognl 语法构造 方便在watch 参数中直接利用哦 ~
 * https://github.com/WangJi92/arthas-idea-plugin/issues/5
 * <p>
 * watch com.test.pandora.eagleeye.HomeController home '@com.taobao.eagleeye.EagleEye@getTraceId()'
 *
 * @author 汪小哥
 * @xxxClass@field or method
 * @date 20-06-2020
 */
public class ArthasOgnlStaticGrammaticalStructureCommandAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        DataContext dataContext = e.getDataContext();
        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        if (editor == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        //获取当前事件触发时，光标所在的元素
        PsiElement psiElement = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
        if (psiElement == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        if (psiElement instanceof PsiClass) {
            e.getPresentation().setEnabled(true);
            return;
        }
        e.getPresentation().setEnabled(true);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        /**
         * {@link com.intellij.ide.actions.CopyReferenceAction}
         */
        DataContext dataContext = event.getDataContext();
        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (editor == null || project == null) {
            return;
        }
        PsiElement psiElement = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
        String className = "";
        String methodName = "";

        StringBuilder builder = new StringBuilder("");
        boolean iStaticMethod = true;
        boolean iStaticField = true;

        // 下面处理 不是static的方法、字段直接获取 方法、字段所在class的 @xxxclass@class 这样的静态信息
        //region 非静态的处理方式
        if (psiElement instanceof PsiMethod) {
            PsiMethod psiMethod = (PsiMethod) psiElement;
            if (psiMethod.getContainingClass() instanceof PsiAnonymousClass) {
                NotifyUtils.notifyMessage(project, "匿名类不支持 使用sc -d xxxClas*$* 查找具体的类处理", NotificationType.ERROR);
                return;
            }
            className = OgnlPsUtils.getCommonOrInnerOrAnonymousClassName(psiMethod);
            if (!psiMethod.hasModifierProperty(PsiModifier.STATIC)) {
                iStaticMethod = false;
                builder.append("@").append(className).append("@").append("class");
            }
        }
        if (psiElement instanceof PsiField) {
            PsiField psiField = (PsiField) psiElement;
            if (psiField.getContainingClass() instanceof PsiAnonymousClass) {
                NotifyUtils.notifyMessage(project, "匿名类不支持 使用sc -d xxxClas*$* 查找具体的类处理", NotificationType.ERROR);
                return;
            }
            className = OgnlPsUtils.getCommonOrInnerOrAnonymousClassName(psiField);
            if (!psiField.hasModifierProperty(PsiModifier.STATIC)) {
                iStaticField = false;
                builder.append("@").append(className).append("@").append("class");
            }
        }
        if (psiElement instanceof PsiClass) {
            PsiClass psiClass = (PsiClass) psiElement;
            className = OgnlPsUtils.getCommonOrInnerOrAnonymousClassName(psiClass);
            builder.append("@").append(className).append("@").append("class");
        }
        //endregion

        //region 静态的处理方式
        if (psiElement instanceof PsiMethod && iStaticMethod) {
            PsiMethod psiMethod = (PsiMethod) psiElement;
            className = OgnlPsUtils.getCommonOrInnerOrAnonymousClassName(psiMethod);
            String methodParameterDefault = OgnlPsUtils.getMethodParameterDefault(psiMethod);
            builder.append("@").append(className).append("@").append(methodParameterDefault);
        }

        if (psiElement instanceof PsiField && iStaticField) {
            PsiField psiField = (PsiField) psiElement;
            if (!psiField.hasModifierProperty(PsiModifier.STATIC)) {
                return;
            }

            className = OgnlPsUtils.getCommonOrInnerOrAnonymousClassName(psiField);
            String fileName = psiField.getNameIdentifier().getText();
            builder.append("@").append(className).append("@").append(fileName);
        }
        //endregion
        ClipboardUtils.setClipboardString(builder.toString());
        NotifyUtils.notifyMessage(project, "watch controller home '@com.E@getTraceId()',类似这种，协助构造后面的部分静态的表达式,还不能直接使用");
    }
}
