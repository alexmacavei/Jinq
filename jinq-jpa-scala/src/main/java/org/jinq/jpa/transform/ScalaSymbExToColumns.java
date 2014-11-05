package org.jinq.jpa.transform;

import java.util.ArrayList;
import java.util.List;

import org.jinq.jpa.jpqlquery.BinaryExpression;
import org.jinq.jpa.jpqlquery.ColumnExpressions;
import org.jinq.jpa.jpqlquery.ConstantExpression;
import org.jinq.jpa.jpqlquery.Expression;
import org.jinq.jpa.jpqlquery.FunctionExpression;
import org.jinq.jpa.jpqlquery.JPQLQuery;
import org.jinq.jpa.jpqlquery.ReadFieldExpression;
import org.jinq.jpa.jpqlquery.RowReader;
import org.jinq.jpa.jpqlquery.ScalaTupleRowReader;
import org.jinq.jpa.jpqlquery.SelectFromWhere;
import org.jinq.jpa.jpqlquery.SelectOnly;
import org.jinq.jpa.jpqlquery.SimpleRowReader;
import org.jinq.jpa.jpqlquery.SubqueryExpression;
import org.jinq.jpa.jpqlquery.TupleRowReader;
import org.objectweb.asm.Type;

import ch.epfl.labos.iu.orm.queryll2.path.TransformationClassAnalyzer;
import ch.epfl.labos.iu.orm.queryll2.symbolic.LambdaFactory;
import ch.epfl.labos.iu.orm.queryll2.symbolic.MethodCallValue;
import ch.epfl.labos.iu.orm.queryll2.symbolic.MethodSignature;
import ch.epfl.labos.iu.orm.queryll2.symbolic.TypedValueVisitorException;

public class ScalaSymbExToColumns extends SymbExToColumns
{
   ScalaSymbExToColumns(JPQLQueryTransformConfiguration config,
         SymbExArgumentHandler argumentHandler)
   {
      super(config, argumentHandler);
   }

   @Override public ColumnExpressions<?> virtualMethodCallValue(MethodCallValue.VirtualMethodCallValue val, SymbExPassDown in) throws TypedValueVisitorException
   {
      MethodSignature sig = val.getSignature();
      if (ScalaMetamodelUtil.newTuple2.equals(sig)
            || ScalaMetamodelUtil.newTuple3.equals(sig)
            || ScalaMetamodelUtil.newTuple4.equals(sig)
            || ScalaMetamodelUtil.newTuple5.equals(sig)
            || ScalaMetamodelUtil.newTuple8.equals(sig))
      {
         ColumnExpressions<?> [] vals = new ColumnExpressions<?> [val.args.size()];
         // TODO: This is a little wonky passing down isExpectingConditional, but I think it's right for those times you create a tuple with booleans and then extract the booleans later
         SymbExPassDown passdown = SymbExPassDown.with(val, in.isExpectingConditional);
         for (int n = 0; n < vals.length; n++)
            vals[n] = val.args.get(n).visit(this, passdown);
         RowReader<?> [] valReaders = new RowReader[vals.length];
         for (int n = 0; n < vals.length; n++)
            valReaders[n] = vals[n].reader;

         ColumnExpressions<?> toReturn = new ColumnExpressions<>(ScalaTupleRowReader.createReaderForTuple(sig.owner, valReaders));
         for (int n = 0; n < vals.length; n++)
            toReturn.columns.addAll(vals[n].columns);
         return toReturn;
      }
      else if (ScalaMetamodelUtil.TUPLE_ACCESSORS.containsKey(sig))
      {
         int idx = ScalaMetamodelUtil.TUPLE_ACCESSORS.get(sig) - 1;
         // TODO: This is a little wonky passing down isExpectingConditional, but I think it's right for those times you create a tuple with booleans and then extract the booleans later
         SymbExPassDown passdown = SymbExPassDown.with(val, in.isExpectingConditional);
         ColumnExpressions<?> base = val.base.visit(this, passdown);
         RowReader<?> subreader = ((ScalaTupleRowReader<?>)base.reader).getReaderForIndex(idx);
         ColumnExpressions<?> toReturn = new ColumnExpressions<>(subreader);
         int baseOffset = ((ScalaTupleRowReader<?>)base.reader).getColumnForIndex(idx);
         for (int n = 0; n < subreader.getNumColumns(); n++)
            toReturn.columns.add(base.columns.get(n + baseOffset));
         return toReturn;
      }
      else
         return super.virtualMethodCallValue(val, in);
   }

}
