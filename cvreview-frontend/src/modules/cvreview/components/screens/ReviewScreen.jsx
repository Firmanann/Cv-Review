import StepIndicator from '../parts/StepIndicator';
import HeroHeading from '../parts/HeroHeading';
import InspirationQuote from '../parts/InspirationQuote';
import ProcessingCard from '../parts/ProcessingCard';

export default function ReviewScreen({ progress }) {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 lg:gap-16 items-start">
      {/* Left column: messaging */}
      <div className="pt-4">
        <StepIndicator step="02" label="PROCESS" />
        <HeroHeading
          title="Now we're working through it."
          subtitle="Our system is parsing the provided context, structuring the information, and preparing the necessary outputs for your review."
        />
        <InspirationQuote />
      </div>

      {/* Right column: processing status */}
      <div className="pt-4">
        <ProcessingCard progress={progress} />
      </div>
    </div>
  );
}
