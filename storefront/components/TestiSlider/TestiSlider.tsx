import { FC, useCallback, useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import LeftArrow from "../../public/icons/LeftArrow";
import RightArrow from "../../public/icons/RightArrow";

type TestimonialItem = {
  speech: string;
  name: string;
  occupation: string;
};

type Props = {
  items?: TestimonialItem[];
};

// animate__fadeIn
// animate__lightSpeedInRight
const TestiSlider: FC<Props> = ({ items = [] }) => {
  const t = useTranslations("Category");
  const testi = items.length
    ? items
    : [
        {
          speech: t("no_reviews"),
          name: "Clothing",
          occupation: t("customer_reviews"),
        },
      ];
  const [arrIndex, setArrIndex] = useState(0);
  const [animate, setAnimate] = useState("animate__lightSpeedInRight");

  const handleNext = useCallback(() => {
    if (!testi.length) return;
    if (arrIndex === testi.length - 1) {
      setArrIndex(0);
    } else {
      setArrIndex((prevState) => prevState + 1);
      setAnimate("animate__lightSpeedInRight");
    }
  }, [arrIndex, testi.length]);

  const handlePrev = () => {
    if (!testi.length) return;
    if (arrIndex === 0) {
      setArrIndex(testi.length - 1);
    } else {
      setArrIndex((prevState) => prevState - 1);
      setAnimate("animate__lightSpeedInLeft");
    }
  };

  useEffect(() => {
    if (testi.length <= 1) return;
    const interval = setInterval(() => {
      handleNext();
    }, 4000);
    return () => clearInterval(interval);
  }, [handleNext, testi.length]);

  useEffect(() => {
    if (arrIndex > testi.length - 1) {
      setArrIndex(0);
    }
  }, [arrIndex, testi.length]);
  return (
    <div
      className="flex flex-1 overflow-hidden relative my-6"
      style={{ width: "700px" }}
    >
      <div className="slide-section min-w-min h-40 flex">
        {testi.map((ti, index) => {
          return (
            index === arrIndex && (
              <div
                key={ti.name}
                className={`h-full flex flex-col items-center justify-center animate__animated ${animate}`}
                style={{ width: "700px" }}
              >
                <div className="textiContainer text-center w-3/4">
                  <span>{ti.speech}</span>
                  <h3 className="font-bold mt-6">{ti.name}</h3>
                  <span className="text-sm">({ti.occupation})</span>
                </div>
              </div>
            )
          );
        })}
      </div>
      <span
        className="absolute top-1/3 left-3 hover:bg-green rounded-full p-2 cursor-pointer outline-none"
        onClick={handlePrev}
      >
        <LeftArrow />
      </span>
      <span
        className="absolute top-1/3 right-5 hover:bg-green rounded-full p-2 cursor-pointer outline-none"
        onClick={handleNext}
      >
        <RightArrow />
      </span>
    </div>
  );
};

export default TestiSlider;
